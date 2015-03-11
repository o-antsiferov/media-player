package bo.pic.android.media.content;

import javax.annotation.Nonnull;

/**
 * Defines contract for an entity which {@link #repaint(MediaContent) exposes canvas} to draw on.
 * <p/>
 * Standard android drawables use the following sequence when their UI should be re-drawn:
 * <ol>
 *   <li>call {@link android.graphics.drawable.Drawable#invalidateSelf()};</li>
 *   <li>wait until the call above is processed by the OS and {@link android.graphics.drawable.Drawable#draw(android.graphics.Canvas)} is called some time at the future;</li>
 * </ol>
 * The problem here is that we can't be sure about the delay between {@link android.graphics.drawable.Drawable#invalidateSelf()} and {@link android.graphics.drawable.Drawable#draw(android.graphics.Canvas)}.
 * That means that that approach is not good for, e.g. playing video frames because we want to preserve particular FPS.
 * <p/>
 * This interface defines an entity which allows {@link #repaint(MediaContent) immediate re-draw}.
 */
public interface RepaintContext {

    /**
     * Asks current entity to perform drawing iteration.
     * <p/>
     * The implementation is supposed to provide given callback a {@link android.graphics.Canvas} object to draw on and then its content are supposed
     * to be immediately shown on screen.
     * <p/>
     * <b>Note:</b> it's supposed that this method is not called until previous call returns control (e.g. don't call it from more than
     * one thread simultaneously).
     *
     * @param content    an interface that encapsulates custom drawing logic
     */
    void repaint(@Nonnull MediaContent content);
}
