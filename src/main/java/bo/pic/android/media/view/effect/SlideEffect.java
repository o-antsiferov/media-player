package bo.pic.android.media.view.effect;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.content.MediaContent;

public class SlideEffect extends AbstractEffect {

    @Nonnull private final Rect mRect = new Rect();

    @Nullable private MediaContent mOldContent;
    @Nullable private MediaContent mCurrentContent;

    @Override
    protected void doOnContentChange(@Nullable MediaContent newContent) {
        if (mOldContent != null && !isFinished()) {
            mOldContent.decrementUsageCounter();
        }
        mOldContent = mCurrentContent;
        if (mCurrentContent != null) {
            mCurrentContent.incrementUsageCounter();
        }

        mCurrentContent = newContent;
    }

    @Override
    protected void onAnimationFinish(@Nonnull View contentView) {
        if (mOldContent != null) {
            mOldContent.decrementUsageCounter();
        }
    }

    @Override
    protected void onDraw(@Nonnull Canvas canvas, @Nonnull MediaContent content, @Nonnull View contentView, long elapsedTimeMillis) {
        Rect redrawBounds = getBounds(canvas, contentView);
        int height = contentView.getHeight();
        int currentContentHeight = (int) (elapsedTimeMillis * height / DURATION_MILLIS);

        mRect.left = redrawBounds.left;
        mRect.right = redrawBounds.right;
        mRect.top = redrawBounds.top;
        mRect.bottom = Math.min(redrawBounds.bottom, currentContentHeight);
        if (mRect.bottom > mRect.top) {
            content.draw(canvas, mRect, getPaint());
        }

        if (mOldContent != null) {
            mRect.top = mRect.bottom;
            mRect.bottom = redrawBounds.bottom;
            if (mRect.bottom > mRect.top) {
                mOldContent.draw(canvas, mRect, getPaint());
            }
        }
    }
}
