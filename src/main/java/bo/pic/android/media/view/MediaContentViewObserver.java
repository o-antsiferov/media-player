package bo.pic.android.media.view;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.content.MediaContent;

/**
 * Holds callbacks for various media content processing-related events.
 */
public interface MediaContentViewObserver {

    void beforeContentChange(@Nonnull MediaContentView view, @Nullable MediaContent newContent);
}
