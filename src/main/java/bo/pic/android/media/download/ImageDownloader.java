package bo.pic.android.media.download;

import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import bo.pic.android.media.util.ProcessingCallback;

public interface ImageDownloader {

    /**
     * Allows to trigger downloading from the given uri notifying given callback about the result.
     * <p/>
     * <b>Note:</b> the processing is expected to be asynchronous, i.e. calling thread is expected to be freed immediately.
     *
     * @param imageUri    target uri to download from
     * @param callback    callback to notify about download result
     * @return            a handle which might be used to {@link java.util.concurrent.Future#cancel(boolean) cancel} download request
     */
    @Nonnull
    Future<?> download(@Nonnull String imageUri, @Nonnull ProcessingCallback<byte[]/* image data */> callback);
}
