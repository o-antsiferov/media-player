package bo.pic.android.media;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.content.MediaContent;

/**
 * Image downloading callback.
 */
interface ImageLoadListener {

    /**
     * Notifies about successful download end.
     * <p/>
     * Is <b>not</b> guaranteed to be called from a main thread.
     *
     * @param handle               a handle created for a download request
     * @param downloadedContent    downloaded content
     */
    void onResponse(@Nonnull ImageLoader.LoadHandle handle, @Nonnull MediaContent downloadedContent);

    /**
     * Notifies about unsuccessful download request completion.
     * <p/>
     * Is <b>not</b> guaranteed to be called from a main thread.
     *
     * @param handle    a handle created for a download request
     * @param ex        an exception occurred during downloading
     */
    void onError(@Nonnull ImageLoader.LoadHandle handle, @Nullable Throwable ex);
}