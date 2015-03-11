package bo.pic.android.media.content;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StaticImageContent extends AbstractMediaContent {

    @Nonnull private final Bitmap mBitmap;

    public StaticImageContent(@Nonnull String contentUri, @Nonnull Bitmap bitmap) {
        super(contentUri);
        mBitmap = bitmap;
    }

    @Override
    public void invite(@Nonnull MediaContentVisitor visitor) {
        visitor.visit(this);
    }

    @Nonnull
    public Bitmap getBitmap() {
        return mBitmap;
    }

    @Override
    protected void doStartDrawingFor(@Nonnull RepaintContext context) {
        if (getActiveContexts().add(context)) {
            context.repaint(this);
        }
    }

    @Override
    protected void doStopDrawing() {
    }

    @Override
    public void release() {
        // No-op.
    }

    @Override
    public void draw(@Nonnull Canvas canvas, @Nonnull Rect clipBounds, @Nullable Paint paint) {
        if (clipBounds.isEmpty()) {
            return;
        }
        canvas.drawBitmap(mBitmap, clipBounds, clipBounds, paint);
    }

    @Override
    public boolean isReleased() {
        return false;
    }

    @Override
    public String toString() {
        return System.identityHashCode(this) + ", content uri: " + getContentUri();
    }
}
