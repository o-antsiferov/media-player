package bo.pic.android.media.view.effect;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.content.MediaContent;

public class FadeInEffect extends AbstractEffect {
    public static final float DEFAULT_MIN_ALPHA = 0.01f;
    public static final float DEFAULT_MAX_ALPHA = 1;

    private final Paint mPaint = new Paint();
    private final float mMinAlpha;
    private final float mMaxAlpha;

    public FadeInEffect() {
        this(DEFAULT_MIN_ALPHA, DEFAULT_MAX_ALPHA);
    }

    public FadeInEffect(float minAlpha, float maxAlpha) {
        mMinAlpha = minAlpha;
        mMaxAlpha = maxAlpha;
    }

    @Override
    protected Paint getPaint() {
        return mPaint;
    }

    @Override
    protected void doOnContentChange(@Nullable MediaContent newContent) {
    }

    @Override
    protected void onAnimationFinish(@Nonnull View contentView) {
        mPaint.setAlpha(Math.round(mMaxAlpha * 255));
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas, @Nonnull MediaContent content, @Nonnull View contentView, long elapsedTimeMillis) {
        if (elapsedTimeMillis > 0) {
            mPaint.setAlpha(Math.round(elapsedTimeMillis * (mMaxAlpha - mMinAlpha) / DURATION_MILLIS * 255));
        } else {
            mPaint.setAlpha(Math.round(mMinAlpha * 255));
        }

        content.draw(canvas, getBounds(canvas, contentView), getPaint());
    }
}
