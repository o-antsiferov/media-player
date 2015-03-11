package bo.pic.android.media.view.effect;

import android.graphics.Canvas;
import android.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.content.MediaContent;

/**
 * Stands for an entity which has a right to adjust target {@link MediaContent} drawing (e.g. use particular effect when the content is
 * updated).
 * <p/>
 * <b>Note:</b> instances of this interface are expected to be bound to a single view which inner {@link MediaContent} might be changed.
 */
public interface Effect {

    /**
     * Notifies current effect about change of the content embedded to the target view
     *
     * @param newContent    new content which is embedded to the target view (<code>null</code> as an indication that the content is reset)
     */
    void onContentChange(@Nullable MediaContent newContent);

    /**
     * This is the core method of this interface. An implementation is expected to show any effect to the target content like
     * fade-in, slide etc.
     *
     * @param canvas         canvas to draw at
     * @param content        content being drawn
     * @param contentView    view which holds given content
     */
    void draw(@Nonnull Canvas canvas, @Nonnull MediaContent content, @Nonnull View contentView);
}
