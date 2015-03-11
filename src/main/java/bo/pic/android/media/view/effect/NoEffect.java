package bo.pic.android.media.view.effect;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.content.MediaContent;

public class NoEffect implements Effect {
    private final Rect mBounds = new Rect();

    protected Rect getBounds(Canvas canvas, View view) {
        mBounds.set(0, 0, view.getWidth(), view.getHeight());
        return mBounds;
    }

    @Override
    public void draw(@Nonnull Canvas canvas, @Nonnull MediaContent content, @Nonnull View contentView) {
        content.draw(canvas, getBounds(canvas, contentView), null);
    }

    @Override
    public void onContentChange(@Nullable MediaContent newContent) {
        // nothing to do
    }
}
