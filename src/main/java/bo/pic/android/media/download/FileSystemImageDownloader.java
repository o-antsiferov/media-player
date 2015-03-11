package bo.pic.android.media.download;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import bo.pic.android.media.util.FileUtil;
import bo.pic.android.media.util.ImageUtil;
import bo.pic.android.media.util.IoUtil;
import bo.pic.android.media.util.ProcessingCallback;

public class FileSystemImageDownloader implements ImageDownloader {
    private final ExecutorService mExecutor;

    public FileSystemImageDownloader(ExecutorService executor) {
        this.mExecutor = executor;
    }

    @Nonnull
    @Override
    public Future<?> download(@Nonnull final String imageUri, @Nonnull final ProcessingCallback<byte[]> callback) {
        return mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                File file = FileUtil.getFile(imageUri);
                if (file == null) {
                    // There is a possible case that we want to show a chat message with a camera photo until the photo is uploaded.
                    // We just use the data from the local photo file then. However, the thing is that we automatically scale chat
                    // message media content if necessary for better user experience. So, it might use dedicated image flavor for that.
                    // That flavor has a different uri (not equal to the original image uri). That's why we try to load by
                    // 'original image uri' if given uri is a one which points not to original image but to one of the image's flavor.
                    String basePath = ImageUtil.getBaseUri(imageUri);
                    if (!basePath.equals(imageUri)) {
                        file = FileUtil.getFile(basePath);
                    }
                }
                if (file == null) {
                    callback.onFail(new IllegalArgumentException("Can't load data from non-existing file " + imageUri));
                    return;
                }

                try {
                    callback.onSuccess(IoUtil.toByteArray(file));
                } catch (IOException exception) {
                    callback.onFail(exception);
                }
            }
        });
    }
}
