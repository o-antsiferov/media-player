package bo.pic.android.media.view;

import android.graphics.drawable.Drawable;

import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.content.MediaContent;
import bo.pic.android.media.util.Key;

/**
 * Defines contract for an entity which might manage {@link bo.pic.android.media.content.MediaContent}
 */
public interface MediaContentView {

    @Nullable
    String getEmbeddedAnimationUri();

    void setMediaContent(@Nullable MediaContent content, boolean forceStart);

    @Nullable
    MediaContent getMediaContent();

    @Nonnull
    ConcurrentMap<Key<?>, ?> getAdditionalData();

    /**
     * Can be called from different threads, but actual placeholder setting must be done in UI thread.
     * Subclasses should handle it by itself.
     *
     * @param placeholder Placeholder drawable to set.
     */
    void setPlaceholder(@Nullable Drawable placeholder);
}
