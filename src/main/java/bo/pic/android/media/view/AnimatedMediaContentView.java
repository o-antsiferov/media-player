package bo.pic.android.media.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.content.MediaContent;
import bo.pic.android.media.content.RepaintContext;
import bo.pic.android.media.util.Key;
import bo.pic.android.media.view.effect.Effect;
import bo.pic.android.media.view.effect.FadeInEffect;

/**
 * This class has two main responsibilities:
 * <ul>
 *   <li>
 *       it's a {@link RepaintContext} for the {@link MediaContentView#setMediaContent(MediaContent, boolean) encapsulated media content} (allows
 *       immediate {@link android.view.TextureView}-backed repaint);
 *   </li>
 *   <li>it IS-A {@link android.view.View}, i.e. it might be added to standard androids UI;</li>
 * </ul>
 */
public class AnimatedMediaContentView extends View implements MediaContentView, RepaintContext {

    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    private final Runnable mRedrawTask = new Runnable() {
        @Override
        public void run() {
            invalidate();
        }
    };

    private final ConcurrentMap<Key<?>, ?> mAdditionalData = new ConcurrentHashMap<>();

    private Effect mEffect = new FadeInEffect();
    private Rect   mBounds = new Rect();

    @Nullable private volatile Drawable     mPlaceholder;
    @Nullable private          MediaContent mContent;

    @Nullable MediaContentViewObserver mObserver;

    private boolean mAttachedToWindow;
    private boolean mPendingStartDrawing;
    private boolean mPaused;

    public AnimatedMediaContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setMediaContentObserver(final MediaContentViewObserver observer) {
        mObserver = observer;
    }

    @Nonnull
    @Override
    public ConcurrentMap<Key<?>, ?> getAdditionalData() {
        return mAdditionalData;
    }

    @Override
    public void setPlaceholder(@Nullable Drawable placeholder) {
        mPlaceholder = placeholder;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            mRedrawTask.run();
        } else {
            sHandler.post(mRedrawTask);
        }
    }

    @Override
    public void repaint(@Nonnull MediaContent content) {
        sHandler.postAtFrontOfQueue(mRedrawTask);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        MediaContent content = mContent;
        if (content == null) {
            drawPlaceholder(canvas);
            return;
        }
        drawEffect(canvas, content);
    }

    protected void drawPlaceholder(@Nonnull Canvas canvas) {
        Drawable placeholder = mPlaceholder;
        Rect bounds = mBounds;
        canvas.getClipBounds(bounds);
        if (placeholder != null && !bounds.isEmpty()) {
            placeholder.setBounds(bounds);
            placeholder.draw(canvas);
        }
    }

    protected void drawEffect(@Nonnull Canvas canvas, @Nonnull MediaContent content) {
        mEffect.draw(canvas, content, this);
    }

    /**
     * Updates animation which uses current view for painting.
     * <p/>
     * <b>Note:</b> must be called from the main thread.
     *
     * @param content    animation
     * @param forceStart if animation should start automatically
     */
    public void setMediaContent(@Nullable MediaContent content, boolean forceStart) {
        if (mObserver != null) {
            mObserver.beforeContentChange(this, content);
        }

        mEffect.onContentChange(content);
        final MediaContent oldContent = mContent;
        if (oldContent != null) {
            oldContent.removeRepaintContext(this);
        }

        mContent = content;
        if (content == null) {
            mPendingStartDrawing = false;
            invalidate();
            return;
        }
        content.addRepaintContext(this);
        if (forceStart) {
            startDrawing();
        } else {
            invalidate();
        }
    }

    @Nullable
    @Override
    public MediaContent getMediaContent() {
        return mContent;
    }

    public void setEffect(@Nonnull Effect effect) {
        mEffect = effect;
    }

    public boolean isDrawingInProgress() {
        MediaContent content = mContent;
        return content != null && content.isDrawingEnabledFor(this);
    }

    public void startDrawing() {
        mPendingStartDrawing = mPaused;
        MediaContent content = mContent;
        if (content != null && mAttachedToWindow && !mPendingStartDrawing) {
            content.startDrawingFor(this);
        }
    }

    public void stopDrawing() {
        mPendingStartDrawing = false;
        MediaContent content = mContent;
        if (content != null) {
            content.stopDrawingFor(this);
        }
    }

    @Nullable
    public String getEmbeddedAnimationUri() {
        MediaContent content = mContent;
        return (content == null || content.isReleased()) ? null : content.getContentUri();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;
        if (mContent != null) {
            startDrawing();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;
        setMediaContent(null, false);
    }

    @Override
    protected void onVisibilityChanged(@Nonnull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        MediaContent content = mContent;
        if (content == null) {
            return;
        }
        if (visibility == VISIBLE) {
            startDrawing();
        } else {
            stopDrawing();
        }
    }

    public void pause() {
        if (mPaused) {
            return;
        }
        mPaused = true;
        boolean drawingWasInProgressOnScrollStart = isDrawingInProgress();
        stopDrawing();
        mPendingStartDrawing = drawingWasInProgressOnScrollStart;
    }

    public void resume() {
        if (!mPaused) {
            return;
        }
        mPaused = false;
        if (mPendingStartDrawing) {
            startDrawing();
        }
    }

    @Override
    public String toString() {
        return System.identityHashCode(this) + ", content: " + mContent + ", alpha: " + getAlpha();
    }
}