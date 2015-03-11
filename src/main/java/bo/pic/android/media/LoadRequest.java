package bo.pic.android.media;

import android.content.Context;
import android.graphics.drawable.Drawable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.content.MediaContent;
import bo.pic.android.media.content.presenter.MediaContentPresenter;
import bo.pic.android.media.content.presenter.SimpleMediaContentPresenter;
import bo.pic.android.media.util.Logger;
import bo.pic.android.media.util.NetUtil;
import bo.pic.android.media.util.ProcessingCallback;
import bo.pic.android.media.util.ScaleMode;
import bo.pic.android.media.util.ThreadUtil;
import bo.pic.android.media.view.MediaContentView;

class LoadRequest {

    @Nonnull private final String mImageUri;
    @Nonnull private final MediaContentType mMediaContentType;
    @Nonnull private final Dimensions mDimensions;
    @Nonnull private final ImageLoadListener mListener;

    @Nullable private final Drawable mPlaceholderDrawable;
    @Nullable private final ProcessingCallback<MediaContent> mCallback;
    @Nonnull private final ScaleMode mScaleMode;

    LoadRequest(@Nonnull final Builder builder, @Nullable final MediaContentView view) {
        mImageUri = buildImageUri(builder);
        mMediaContentType = builder.mMediaContentType;
        mPlaceholderDrawable = buildPlaceholderDrawable(builder);
        mDimensions = buildDimensions(builder);
        mCallback = builder.mSuccessCallback;
        mScaleMode = builder.mScaleMode;
        mListener = createImageLoadListener(builder, view);
    }

    @Nonnull
    private String buildImageUri(@Nonnull Builder builder) {
        return builder.mImageUri == null ? "" : NetUtil.normalizeUri(builder.mImageUri);
    }

    @Nonnull
    private Dimensions buildDimensions(@Nonnull Builder builder) {
        return new Dimensions(getWidthToUse(builder.mWidth), getHeightToUse(builder.mHeight));
    }

    @Nullable
    private Drawable buildPlaceholderDrawable(@Nonnull Builder builder) {
        return builder.mPlaceholderResId == 0
               ? builder.mPlaceholderDrawable
               : builder.mApplicationContext.getResources().getDrawable(builder.mPlaceholderResId);
    }

    private int getWidthToUse(int width) {
        int widthToUse = width;
        if (widthToUse <= 0 && mPlaceholderDrawable != null) {
            widthToUse = mPlaceholderDrawable.getMinimumWidth();
        }
        return widthToUse;
    }

    private int getHeightToUse(int height) {
        int heightToUse = height;
        if (heightToUse <= 0 && mPlaceholderDrawable != null) {
            heightToUse = mPlaceholderDrawable.getMinimumHeight();
        }
        return heightToUse;
    }

    @Nonnull
    private ImageLoadListener createImageLoadListener(@Nonnull final Builder builder, @Nullable final MediaContentView view) {
        return new ImageLoadListener() {
            @Override
            public void onResponse(@Nonnull ImageLoader.LoadHandle handle, @Nonnull final MediaContent downloadedContent) {
                ThreadUtil.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (view != null) {
                            builder.mPresenter.setMediaContent(downloadedContent, view);
                        }
                        if (mCallback != null) {
                            mCallback.onSuccess(downloadedContent);
                        }
                    }
                });
            }

            @Override
            public void onError(@Nonnull ImageLoader.LoadHandle handle, @Nullable Throwable ex) {
                Logger.w(LoadRequest.class, "Can't download media content from %s", ex, handle.imageUri);
                if (mCallback != null) {
                    mCallback.onFail(ex);
                }
            }
        };
    }

    @Nullable
    public String getImageUri() {
        return mImageUri;
    }

    @Nonnull
    public MediaContentType getMediaContentType() {
        return mMediaContentType;
    }

    @Nullable
    public Drawable getPlaceholderDrawable() {
        return mPlaceholderDrawable;
    }

    @Nonnull
    public Dimensions getDimensions() {
        return mDimensions;
    }

    @Nonnull
    public ScaleMode getScaleMode() {
        return mScaleMode;
    }

    @Nonnull
    public ImageLoadListener getListener() {
        return mListener;
    }

    @Override
    public String toString() {
        return mImageUri + " " + mDimensions.getWidth() + "x" + mDimensions.getHeight() + " " + mMediaContentType;
    }


    public static class Builder {
        @Nonnull private final Context mApplicationContext;
        @Nonnull private final ImageLoader mImageLoader;
        @Nullable private final String mImageUri;
        @Nonnull private final MediaContentType mMediaContentType;

        @Nullable private Drawable mPlaceholderDrawable;
        @Nullable private ProcessingCallback<MediaContent> mSuccessCallback;

        private ScaleMode             mScaleMode = ScaleMode.FIT;
        private MediaContentPresenter mPresenter = new SimpleMediaContentPresenter();

        private int mPlaceholderResId;
        private int mWidth;
        private int mHeight;

        Builder(@Nonnull Context applicationContext,
                @Nonnull ImageLoader imageLoader,
                @Nullable String imageUri,
                @Nonnull MediaContentType mediaContentType)
        {
            mApplicationContext = applicationContext;
            mImageLoader = imageLoader;
            mImageUri = imageUri;
            mMediaContentType = mediaContentType;
        }

        @Nonnull
        public Builder setPlaceholder(int placeholderResId) {
            mPlaceholderResId = placeholderResId;
            return this;
        }

        @SuppressWarnings("UnusedDeclaration")
        @Nonnull
        public Builder setPlaceholder(@Nullable Drawable placeholderDrawable) {
            mPlaceholderDrawable = placeholderDrawable;
            return this;
        }

        @SuppressWarnings("UnusedDeclaration")
        @Nonnull
        public Builder setDimensions(int width, int height) {
            mWidth = width;
            mHeight = height;
            return this;
        }

        @Nonnull
        public Builder setProcessingCallback(@Nullable ProcessingCallback<MediaContent> callback) {
            mSuccessCallback = callback;
            return this;
        }

        @Nonnull
        public Builder setPresenter(@Nonnull MediaContentPresenter mediaContentPresenter) {
            mPresenter = mediaContentPresenter;
            return this;
        }

        @Nonnull
        public Builder setScaleMode(@Nonnull ScaleMode scaleMode) {
            mScaleMode = scaleMode;
            return this;
        }

        @Nullable
        public ImageLoader.LoadHandle into(@Nonnull MediaContentView view) {
            return into(view, false);
        }

        @Nullable
        public ImageLoader.LoadHandle into(@Nonnull MediaContentView view, boolean resetBeforeLoading) {
            if (resetBeforeLoading) {
                view.setMediaContent(null, false);
            }
            return doLoad(view);
        }

        @Nullable
        public ImageLoader.LoadHandle proceed() {
            return doLoad(null);
        }

        @Nullable
        private ImageLoader.LoadHandle doLoad(@Nullable MediaContentView view) {
            return mImageLoader.processLoadRequest(new LoadRequest(this, view), view);
        }
    }
}
