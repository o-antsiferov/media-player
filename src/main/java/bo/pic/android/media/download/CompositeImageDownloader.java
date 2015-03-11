package bo.pic.android.media.download;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import bo.pic.android.media.util.ProcessingCallback;
import bo.pic.android.media.util.Scheme;

public class CompositeImageDownloader implements ImageDownloader {
    private final Map<Scheme, ImageDownloader> mDownloaders = new HashMap<>();

    public CompositeImageDownloader(@Nonnull FileSystemImageDownloader fileLoader,
                                    @Nonnull HttpAsyncClientImageDownloader networkLoader)
    {
        mDownloaders.put(Scheme.HTTP, networkLoader);
        mDownloaders.put(Scheme.HTTPS, networkLoader);
        mDownloaders.put(Scheme.FILE, fileLoader);
    }

    @Nonnull
    @Override
    public Future<?> download(@Nonnull String imageUri, @Nonnull ProcessingCallback<byte[]> callback) {
        Scheme scheme = Scheme.of(imageUri);
        ImageDownloader loader = mDownloaders.get(scheme);
        if (loader == null) {
            throw new IllegalArgumentException(String.format("Can't download image data from url %s. Reason: unknown protocol scheme. "
                                                             + "Supported schemes: %s", imageUri, mDownloaders.keySet()));
        }
        return loader.download(imageUri, callback);
    }
}
