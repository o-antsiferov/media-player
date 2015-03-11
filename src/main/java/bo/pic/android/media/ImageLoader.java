package bo.pic.android.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.bitmap.BitmapPool;
import bo.pic.android.media.cache.CacheKey;
import bo.pic.android.media.cache.DiskCache;
import bo.pic.android.media.cache.ImageCacheUtils;
import bo.pic.android.media.cache.MemoryCache;
import bo.pic.android.media.content.MediaContent;
import bo.pic.android.media.content.MediaContentVisitor;
import bo.pic.android.media.content.StaticImageContent;
import bo.pic.android.media.content.animation.AnimatedImageContent;
import bo.pic.android.media.content.transformation.MediaContentTransformation;
import bo.pic.android.media.download.ImageDownloader;
import bo.pic.android.media.util.ImageUtil;
import bo.pic.android.media.util.Key;
import bo.pic.android.media.util.ProcessingCallback;
import bo.pic.android.media.util.ScaleMode;
import bo.pic.android.media.view.MediaContentView;

/**
 * Class for image loading/resizing/recycling/caching.
 * <p>
 * Bitmap recycling is only available on Honeycomb and up.
 * </p>
 *
 * @see <a href="http://developer.android.com/training/displaying-bitmaps/manage-memory.html">
 * Manage bitmap memory developers guide</a>
 */
public class ImageLoader {

    public static final Key<LoadHandle> LOAD_HANDLE_KEY = new Key<>("LOAD_HANDLE", LoadHandle.class);

    @Nonnull private final Context mApplicationContext;
    @Nonnull private final ImageDownloader mImageDownloader;
    @Nonnull private final MemoryCache<CacheKey<String>, MediaContent> mMemoryCache;
    @Nonnull private final DiskCache<CacheKey<String/* target data uri */>> mDiskCache;
    @Nonnull private final Map<MediaContentType, MediaContentTransformation> mTransformations;
    @Nonnull private final BitmapPool mBitmapPool;
    @Nonnull private final ExecutorService mImageLoaderExecutor;

    /**
     * Suppose that a request for downloading particular content arrives. We start actual downloading but it takes some time to complete.
     * There is a possible case that another request for the same content arrives. We don't want to start new download then but want
     * to coalesce this request with the previous one and update both of them on download completion.
     * <p/>
     * This map holds information about active downloads which allows us to coalesce new download requests (in terms of
     * {@link LoadHandle} objects) within an actual downloading ({@link DownloadRequest} object).
     */
    private final ConcurrentMap<String/* target uri to download from */, DownloadRequest> mInFlightRequests = new ConcurrentHashMap<>();

    public ImageLoader(@Nonnull Context applicationContext,
                       @Nonnull ImageDownloader imageDownloader,
                       @Nonnull MemoryCache<CacheKey<String>, MediaContent> memoryCache,
                       @Nonnull DiskCache<CacheKey<String>> diskCache,
                       @Nonnull Map<MediaContentType, MediaContentTransformation> transformations,
                       @Nonnull BitmapPool bitmapPool,
                       @Nonnull ExecutorService imageLoaderExecutor)
    {
        mApplicationContext = applicationContext;
        mImageDownloader = imageDownloader;
        mMemoryCache = memoryCache;
        mDiskCache = diskCache;
        mTransformations = transformations;
        mBitmapPool = bitmapPool;
        mMemoryCache.setRemoveFromCacheListener(new MemoryCache.RemoveFromCacheListener<MediaContent>() {
            @Override
            public void onRemoved(MediaContent value) {
                value.invite(new MediaContentVisitor() {
                    @Override
                    public void visit(@Nonnull StaticImageContent content) {
                        mBitmapPool.put(content.getBitmap());
                    }

                    @Override
                    public void visit(@Nonnull AnimatedImageContent content) {
                        content.decrementUsageCounter();
                    }
                });
            }
        });
        mImageLoaderExecutor = imageLoaderExecutor;
    }



    /**
     * Creates an image load request using the specified {@code imageUri}.
     * The {@code imageUri} may be a remote url (prefixed with {@code http://} or {@code https://}) or a file resource (prefixed with
     * {@code file://}).
     * If {@code imageUri} is a {@code null} there will be no any request but a placeholder will be set, if it's specified.
     *
     * @param imageUri Image uri in the following format "http://smth.com/img.png", "file:///mnt/sdcard/img.png")
     */
    @Nonnull
    public LoadRequest.Builder load(@Nullable String imageUri, @Nonnull MediaContentType type) {
        return new LoadRequest.Builder(mApplicationContext, this, imageUri, type);
    }

    @Nullable
    LoadHandle processLoadRequest(@Nonnull LoadRequest loadRequest, @Nullable MediaContentView view) {
        final String imageUri = loadRequest.getImageUri();

        // Get the previous request handle pending on this View (in case this view was recycled).
        final LoadHandle previousHandle = getLoadHandleFromView(view);
        // Get the image uri from the previous request handle.
        final String prevImageUri = previousHandle == null ? null: previousHandle.imageUri;
        if(imageUri != null && imageUri.equals(prevImageUri)) {
            return previousHandle;
        }

        // Cancel the previous download request if it is not null.
        tryToCancelHandle(previousHandle);

        Drawable placeholder = loadRequest.getPlaceholderDrawable();
        if(TextUtils.isEmpty(imageUri) && view != null && placeholder != null) {
            view.setPlaceholder(placeholder);
            return null;
        }

        assert imageUri != null;
        Dimensions dimensions = loadRequest.getDimensions();
        MediaContentType contentType = loadRequest.getMediaContentType();
        final LoadHandle handle = new LoadHandle(dimensions,
                                                 contentType,
                                                 imageUri,
                                                 loadRequest.getScaleMode(),
                                                 view,
                                                 mTransformations.get(contentType),
                                                 loadRequest.getListener());

        // Check if image already available in a memory cache.
        final CacheKey<String> memoryCacheKey = ImageCacheUtils.getMemoryCacheKey(imageUri, contentType, dimensions);
        final MediaContent cachedContent = mMemoryCache.get(memoryCacheKey);
        if (cachedContent != null) {
            ImageLoadListener listener = loadRequest.getListener();
            listener.onResponse(handle, cachedContent);
            return previousHandle;
        }

        if (view != null) {
            LOAD_HANDLE_KEY.put(view.getAdditionalData(), handle);

            // Set placeholder if it's available prior to queue a new download request.
            if (placeholder != null) {
                view.setPlaceholder(placeholder);
            }
        }

        // Queue new download request
        enqueueDownloadRequest(handle);
        return handle;
    }

    private void tryToCancelHandle(@Nullable LoadHandle handle) {
        if (handle != null) {
            handle.cancelDownloadRequest();
        }
    }

    @Nullable
    private LoadHandle getLoadHandleFromView(@Nullable MediaContentView view) {
        return view == null ? null : LOAD_HANDLE_KEY.get(view.getAdditionalData());
    }

    private void enqueueDownloadRequest(@Nonnull LoadHandle handle) {
        // Check if a download request is already in-flight.
        DownloadRequest request = mInFlightRequests.get(handle.imageUri);
        if (request != null) {
            // There is an in-flight request, so just register given handle for the existing request.
            request.attach(handle);
            return;
        }

        // There is no any in-flight request, so queue a new download request to fetch an image.
        final DownloadRequest newRequest = new DownloadRequest(new CacheKey<>(handle.imageUri, handle.contentType), handle);
        DownloadRequest previousRequest = mInFlightRequests.putIfAbsent(handle.imageUri, newRequest);
        if (previousRequest == null) {
            mImageLoaderExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    // Execute the request in a non-UI thread because it contacts to disk cache and we don't want
                    // to perform I/O at the main thread.
                    newRequest.download();
                }
            });
        } else {
            previousRequest.attach(handle);
        }
    }

    /**
     * Every time new {@link #processLoadRequest(LoadRequest, MediaContentView) load request} occurs, new object
     * of this class is created and serves as a representation of that request.
     * <p/>
     * Thread-safe.
     */
    public class LoadHandle {
        @Nonnull final Dimensions dimensions;
        @Nonnull final MediaContentType contentType;
        @Nonnull final String imageUri;
        @Nonnull final ScaleMode scaleMode;

        @Nullable private final MediaContentTransformation mTransformation;
        @Nonnull private final  ImageLoadListener          mListener;
        @Nullable private final MediaContentView           mView;

        LoadHandle(@Nonnull Dimensions dimensions,
                   @Nonnull MediaContentType contentType,
                   @Nonnull String imageUri,
                   @Nonnull ScaleMode scaleMode,
                   @Nullable MediaContentView view,
                   @Nullable MediaContentTransformation transformation,
                   @Nonnull ImageLoadListener listener)
        {
            this.dimensions = dimensions;
            this.scaleMode = scaleMode;
            this.contentType = contentType;
            this.imageUri = imageUri;
            this.mView = view;
            this.mTransformation = transformation;
            this.mListener = listener;
        }

        public void cancelDownloadRequest() {
            DownloadRequest request = mInFlightRequests.get(imageUri);
            if (request != null) {
                request.detach(this);
            }
        }

        void onResponse(@Nonnull MediaContent downloadedContent) {
            if (mView != null) {
                mView.getAdditionalData().remove(LOAD_HANDLE_KEY, this);
            }
            mListener.onResponse(this, downloadedContent);
        }

        void onError(@Nullable Throwable ex) {
            if (mView != null) {
                mView.getAdditionalData().remove(LOAD_HANDLE_KEY, this);
            }
            mListener.onError(this, ex);
        }

        @Override
        public String toString() {
            return System.identityHashCode(this) + ": " + imageUri + " " + dimensions;
        }
    }

    /**
     * Represents actual download operation. The main idea is that there might be multiple
     * {@link #processLoadRequest(LoadRequest, MediaContentView) requests to download data from the same uri}.
     * We want to coalesce them (in terms of {@link LoadHandle} objects) and perform actual downloading just one time.
     * <p/>
     * This class aggregates all interested {@link LoadHandle handles} and {@link ImageLoadListener notifies} them on download completion.
     * <p/>
     * Thread-safe.
     */
    private class DownloadRequest implements ProcessingCallback<byte[]> {

        private final Set<LoadHandle> mHandles = Collections.newSetFromMap(new ConcurrentHashMap<LoadHandle, Boolean>());

        @Nonnull private final CacheKey<String> mDiskCacheKey;

        private final AtomicReference<Future<?>> mDownloadHandle = new AtomicReference<Future<?>>();
        private final AtomicReference<byte[]> mDownloaded     = new AtomicReference<byte[]>();
        private final AtomicReference<Throwable> mError          = new AtomicReference<Throwable>();

        public DownloadRequest(@Nonnull CacheKey<String> diskCacheKey,
                               @Nonnull LoadHandle handle)
        {
            mDiskCacheKey = diskCacheKey;
            mHandles.add(handle);
        }

        public void attach(@Nonnull LoadHandle handle) {
            byte[] downloaded = mDownloaded.get();
            if (downloaded != null) {
                final CacheKey<String> memoryCacheKey = ImageCacheUtils.getMemoryCacheKey(mDiskCacheKey.key,
                                                                                          handle.contentType,
                                                                                          handle.dimensions);
                MediaContent content = mMemoryCache.get(memoryCacheKey);
                if (content == null) {
                    try {
                        mMemoryCache.put(memoryCacheKey, content = decodeByteArray(downloaded, handle.dimensions, handle.scaleMode));
                        content.incrementUsageCounter();
                    } catch (Throwable e) {
                        handle.onError(e);
                        return;
                    }
                }
                handle.onResponse(content);
                return;
            }
            Throwable e = mError.get();
            if (e != null) {
                handle.onError(e);
                return;
            }
            mHandles.add(handle);
        }

        public void detach(@Nonnull LoadHandle handle) {
            mHandles.remove(handle);
            if (!mHandles.isEmpty()) {
                return;
            }
            mInFlightRequests.remove(mDiskCacheKey.key, this);
            Future<?> downloadHandle = mDownloadHandle.get();
            if (downloadHandle != null && mDownloadHandle.compareAndSet(downloadHandle, null)) {
                downloadHandle.cancel(true);
            }
        }

        public void download() {
            final byte[] data = mDiskCache.get(mDiskCacheKey);
            if (data != null && data.length > 0) {
                onDownloaded(data);
                return;
            }

            mDownloadHandle.set(mImageDownloader.download(mDiskCacheKey.key, this));
        }

        @Override
        public void onSuccess(@Nonnull byte[] data) {
            if (data.length <= 0) {
                return;
            }
            mDiskCache.put(mDiskCacheKey, data);
            onDownloaded(data);
        }

        @Override
        public void onFail(@Nullable Throwable e) {
            mError.set(e);
            try {
                for (LoadHandle handle : mHandles) {
                    handle.onError(e);
                }
            } finally {
                mInFlightRequests.remove(mDiskCacheKey.key, this);
            }
        }

        private void onDownloaded(@Nonnull byte[] data) {
            mDownloaded.set(data);
            try {
                mInFlightRequests.remove(mDiskCacheKey.key, this);
                Throwable e = null;
                for (LoadHandle handle : mHandles) {
                    final CacheKey<String> memoryCacheKey = ImageCacheUtils.getMemoryCacheKey(mDiskCacheKey.key,
                                                                                              handle.contentType,
                                                                                              handle.dimensions);
                    MediaContent content = mMemoryCache.get(memoryCacheKey);
                    if (content == null) {
                        if (e != null) {
                            handle.onError(e);
                            continue;
                        }
                        try {
                            content = decodeByteArray(data, handle.dimensions, handle.scaleMode);
                            if (handle.mTransformation != null) {
                                content = handle.mTransformation.transform(content);
                            }
                            mMemoryCache.put(memoryCacheKey, content);
                            content.incrementUsageCounter();
                        } catch (Throwable ex) {
                            e = ex;
                            mError.set(e);
                            handle.onError(e);
                            continue;
                        }
                    }
                    handle.onResponse(content);
                }
            } finally {
                mInFlightRequests.remove(mDiskCacheKey.key, this);
            }
        }

        @Nonnull
        private MediaContent decodeByteArray(@Nonnull byte[] data, @Nonnull Dimensions dimensions, @Nonnull ScaleMode scaleMode) {
            //if (ImageUtil.isMp4(data)) {
            //    return new AnimatedImageContent(mDiskCache.getFile(mDiskCacheKey), mDiskCacheKey.key, scaleMode);
            //} else {
            //    final Bitmap bitmap = ImageUtil.decodeBitmap(data,
            //            dimensions.getWidth(),
            //            dimensions.getHeight(),
            //            scaleMode,
            //            Bitmap.Config.RGB_565, mBitmapPool);
            //    return new StaticImageContent(mDiskCacheKey.key, bitmap);
            //}
            return new AnimatedImageContent(mDiskCache.getFile(mDiskCacheKey), mDiskCacheKey.key, scaleMode);
        }

        @Override
        public String toString() {
            return System.identityHashCode(this) + ": " + mDiskCacheKey.key + ", handles: " + mHandles;
        }
    }
}