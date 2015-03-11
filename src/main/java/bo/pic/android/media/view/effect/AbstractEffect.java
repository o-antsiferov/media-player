package bo.pic.android.media.view.effect;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.support.v4.view.ViewCompat;
import android.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.content.MediaContent;

public abstract class AbstractEffect implements Effect {
    public static final long DURATION_MILLIS = 300;

    private final Rect mBounds = new Rect();
    private long mStartEffectTimeMillis;
    private boolean mFinished;

    @Override
    public final void onContentChange(@Nullable MediaContent newContent) {
        doOnContentChange(newContent);
        mStartEffectTimeMillis = -1;
        mFinished = false;
    }

    protected boolean isFinished() {
        return mFinished;
    }

    protected Rect getBounds(Canvas canvas, View view) {
        mBounds.set(0, 0, view.getWidth(), view.getHeight());
        return mBounds;
    }

    protected Paint getPaint() {
        return null;
    }

    @Override
    public final void draw(@Nonnull Canvas canvas, @Nonnull MediaContent content, @Nonnull View contentView) {
        Rect redrawBounds = getBounds(canvas, contentView);

        if (mFinished) {
            content.draw(canvas, redrawBounds, getPaint());
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (mStartEffectTimeMillis <= 0) {
            mStartEffectTimeMillis = now;
        }

        long elapsed = now - mStartEffectTimeMillis;
        if (elapsed >= DURATION_MILLIS) {
            content.draw(canvas, redrawBounds, getPaint());
            mFinished = true;
            onAnimationFinish(contentView);
            return;
        }

        onDraw(canvas, content, contentView, elapsed);
        ViewCompat.postInvalidateOnAnimation(contentView);
    }

    protected abstract void doOnContentChange(@Nullable MediaContent newContent);

    protected abstract void onAnimationFinish(@Nonnull View contentView);

    protected abstract void onDraw(@Nonnull Canvas canvas, @Nonnull MediaContent content, @Nonnull View contentView, long elapsedTimeMillis);
}
