package bo.pic.android.media.content;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Stands for a media content that might be shown within a message (e.g. an animated image, a static image file etc).
 * <p/>
 * Implementations of this interface are expected to be thread-safe.
 */
public interface MediaContent extends AutoReleasable {

    @Nonnull
    String getContentUri();

    /**
     * Registers given repaint context within the current content.
     * <p/>
     * A client must explicitly call {@link #startDrawingFor(RepaintContext) start drawing} if she want to draw on this context.
     *
     * @param context    a repaint context to register within the current content
     * @return           <code>true</code> if given context was not registered within the current context before this method call;
     *                   <code>false</code> otherwise (the context is not registered twice)
     */
    boolean addRepaintContext(@Nonnull RepaintContext context);

    /**
     * De-registers given context from the current content and automatically {@link #stopDrawingFor(RepaintContext) stops drawing} itself
     * on this context.
     *
     * @param context    context to de-register
     * @return           <code>true</code> if given context was registered within the current content;
     *                   <code>false</code> otherwise
     */
    boolean removeRepaintContext(@Nonnull RepaintContext context);

    /**
     * @param context    target context
     * @return           <code>true</code> if drawing is enabled (there was a call to {@link #startDrawingFor(RepaintContext)} and
     *                   no call to {@link #stopDrawingFor(RepaintContext)} or {@link #removeRepaintContext(RepaintContext)} for the
     *                   given context)
     */
    boolean isDrawingEnabledFor(@Nonnull RepaintContext context);

    /**
     * Notifies current content that it should start drawing itself on the previously
     * {@link #addRepaintContext(RepaintContext) added context}.
     *
     * @param context    a context for which drawing should be performed
     */
    void startDrawingFor(@Nonnull RepaintContext context);

    /**
     * Notifies current content that it should start stop drawing itself.
     *
     * @param context    a context for which repainting should not be performed until {@link #startDrawingFor(RepaintContext)} is called
     */
    void stopDrawingFor(@Nonnull RepaintContext context);

    void invite(@Nonnull MediaContentVisitor visitor);

    boolean isReleased();

    void draw(@Nonnull Canvas canvas, @Nonnull Rect clipBounds, @Nullable Paint paint);
}
