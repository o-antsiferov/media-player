package bo.pic.android.media.content.animation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.Dimensions;
import bo.pic.android.media.content.AbstractMediaContent;
import bo.pic.android.media.content.MediaContent;
import bo.pic.android.media.content.MediaContentVisitor;
import bo.pic.android.media.content.RepaintContext;
import bo.pic.android.media.util.ScaleMode;

/**
 * This is a UI-aware wrapper above the purely business-logic {@link AnimationDecoder}.
 * <p/>
 * I.e. {@link AnimationDecoder} provides single-threaded interface for retrieving animation frames and this class handles the following:
 * <ul>
 *      <li>ensures that {@link AnimationDecoder} is accessed from the same thread all the time;</li>
 *      <li>ensures that there is no race condition during {@link RepaintContext#repaint(MediaContent) drawing};</li>
 *      <li>conforms to the {@link MediaContent} interface;</li>
 * </ul>
 */
public class AnimatedImageContent extends AbstractMediaContent implements AnimationDecoder.Callback {

    /**
     * The animation algorithm is as follows:
     * <ol>
     *      <li>{@link #decodeNextFrame() Decode a frame};</li>
     *      <li>{@link #draw(android.graphics.Canvas, android.graphics.Rect, android.graphics.Paint) draw the frame};</li>
     *      <li>{@link #decodeNextFrame() decode next frame};</li>
     *      <li>
     *          Check how much time is left when the next frame is decoded. There are two possible cases:
     *          <ul>
     *              <li>it's too early to draw the next frame;</li>
     *              <li>it's time to draw the next frame;</li>
     *          </ul>
     *          That 'too early' case is backed by the current scheduled executor - {@link AnimatedImageContent drawables} post
     *          {@link java.util.concurrent.ScheduledExecutorService#schedule(Runnable, long, java.util.concurrent.TimeUnit) delayed tasks} here.
     *      </li>
     * </ol>
     * <b>Note:</b> this executor is shared between all drawables, that's why all tasks submitted here are expected
     * to be executed immediately (we need to preserve timings).
     */
    private static final ScheduledExecutorService sExecutor = Executors.newScheduledThreadPool(1);

    /**
     * A queue to use for all animation-related tasks like 'decode next frame', 'reset', 'start/stop animation' etc.
     * <p/>
     * The general idea is to not abuse UI thread by animation processing.
     */
    private static final DecodeQueue mTaskQueue = new DecodeQueue(1);

    private final DecodeTask mDecodeTask = new DecodeTask();
    private final StartTask  mStartTask  = new StartTask();
    private final StopTask   mStopTask   = new StopTask();

    private final Matrix mMatrix = new Matrix();
    private final RectF mRect1 = new RectF();
    private final RectF mRect2 = new RectF();

    @Nonnull private final AnimationDecoder mDecoder;

    @Nullable private DelayedRepaintTask mDelayedRepaintTask;
    @Nullable private RepaintTask        mRepaintTask;

    private final int mThreadId;

    private volatile boolean mRunning;
    private volatile boolean mAnimationReady;

    private ScaleMode mScaleMode;
    private Bitmap mBitmap;
    private Rect mBitmapRect;
    private long    mLastFrameDrawTime;
    private long    mLastFrameOffset;
    private boolean mInitialized;

    /**
     * Calls from a background thread.
     */
    public AnimatedImageContent(@Nonnull File file,
                                @Nonnull String contentUri,
                                @Nonnull ScaleMode scaleMode)
    {
        super(contentUri);
        mThreadId = hashCode();
        mDecoder = new AnimationDecoder(file, this);
        mScaleMode = scaleMode;
    }

    @Override
    public void invite(@Nonnull MediaContentVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected void doStartDrawingFor(@Nonnull RepaintContext context) {
        boolean wasEmpty = getActiveContexts().isEmpty();
        boolean added = getActiveContexts().add(context);
        if(wasEmpty && added) {
            mTaskQueue.add(mStartTask);
        }
    }

    @Override
    protected void doStopDrawing() {
        mTaskQueue.add(mStopTask);
    }

    public boolean isRunning() {
        return mRunning;
    }

    @Override
    public void draw(@Nonnull Canvas canvas, @Nonnull Rect clipBounds, @Nullable Paint paint) {
        if (clipBounds.isEmpty() || !mAnimationReady || mBitmap == null || mBitmap.isRecycled()) {
            return;
        }

        if (paint != null && !paint.isFilterBitmap()) {
            paint.setFilterBitmap(true);
        }

        onPrepareMatrix(mMatrix, mBitmapRect, clipBounds);
        canvas.drawBitmap(mBitmap, mMatrix, paint);
    }

    private void onPrepareMatrix(final Matrix matrix, final Rect bitmap, final Rect clip) {
        mRect1.set(bitmap);
        mRect2.set(clip);

        matrix.setRectToRect(mRect1, mRect2, Matrix.ScaleToFit.CENTER);

        if (mScaleMode == ScaleMode.CROP) {
            final float scaleX = mRect1.width() / mRect2.width();
            final float scaleY = mRect1.height() / mRect2.height();
            final float scale = Math.max(scaleX, scaleY) / Math.min(scaleX, scaleY);

            matrix.postScale(scale, scale, mRect2.centerX(), mRect2.centerY());
        }
    }

    @Override
    public void release() {
        if (isRunning()) {
            doStopDrawing();
        }
        mTaskQueue.add(new ReleaseTask());
    }

    private void decodeNextFrame() {
        mTaskQueue.add(mDecodeTask);
    }

    @Override
    public void onDecoderReset() {
        mLastFrameOffset = 0;
    }

    public void onDecodeTaskCompleted(final long frameOffsetTime) {
        if (!mRunning) {
            return;
        }
        if (!mAnimationReady) {
            mAnimationReady = true;
            repaint();
            return;
        }

        long frameDelay = frameOffsetTime - mLastFrameOffset;
        long delay = (mLastFrameDrawTime + frameDelay) - SystemClock.uptimeMillis();
        mLastFrameOffset = frameOffsetTime;
        if (delay > 0) {
            if (mDelayedRepaintTask != null) {
                sExecutor.schedule(mDelayedRepaintTask, delay, TimeUnit.MILLISECONDS);
            }
        } else {
            repaint();
        }
    }

    private void repaint() {
        for (RepaintContext context : getActiveContexts()) {
            context.repaint(this);
        }
        mLastFrameDrawTime = SystemClock.uptimeMillis();
        decodeNextFrame();
    }

    @Override
    public boolean isReleased() {
        return mDecoder.isReleased();
    }

    @Override
    public String toString() {
        return System.identityHashCode(this) + ", animation uri: " + getContentUri() + ", decoder: " + mDecoder;
    }

    private abstract class AbstractTask implements DecodeQueue.Task {
        @Override
        public int getThreadId() {
            return mThreadId;
        }
    }

    /**
     * Encapsulates logic of {@link #doStartDrawingFor(RepaintContext)} ()}.
     * <p/>
     * We can't perform it directly at {@link #doStartDrawingFor(RepaintContext)} ()} because it changes multiple state elements and, hence,
     * need to be synced with decode thread.
     */
    private class StartTask extends AbstractTask {
        @Override
        public void run() {
            if (!mRunning) {
                mRunning = true;
                // We need to recreate the tasks below because of the tricky problem which occurs during calls to stop() & start() with
                // little delay between them.
                // Normal animation:
                //   1. A frame F1 is decoded;
                //   2. The F1 frame is drawn;
                //   3. Next frame (F2) is decoded;
                //   4. Delayed task is registered for the F2 frame drawing;
                // That means that a single decode task produces endless animation cycle. The problem occurs when stop() & start()
                // are called:
                //   N.   A frame is decoded;
                //   N+1. Delayed task for the frame drawing is registered;
                //   N+2. stop() is called;
                //   N+3. start() is called;
                // The problem here is that start() also enqueues a task to decode a frame, hence, we have two animation loops
                // for the single animation. That results in a visual animation speed increase.
                //
                // The fix is to make 'delayed draw frame' operation aware of it's creation context and reject the processing
                // if current context is different. That is implemented in terms of mRepaintTask reference - it doesn't proceed
                // if current mRepaintTask field references another object and start() method does update the field's value.
                mRepaintTask = new RepaintTask();
                mDelayedRepaintTask = new DelayedRepaintTask(mRepaintTask);
                decodeNextFrame();
            }
        }
    }

    /**
     * @see StartTask
     */
    private class StopTask extends AbstractTask {
        @Override
        public void run() {
            if (mRunning) {
                mRunning = false;
                mRepaintTask = null;
                mDelayedRepaintTask = null;
            }
        }
    }

    private class ReleaseTask extends AbstractTask {
        @Override
        public void run() {
            Bitmap bitmap = mBitmap;
            mBitmap = null;
            if (bitmap != null) {
                bitmap.recycle();
            }
            if (mInitialized) {
                mDecoder.release();
            }
        }
    }

    private class DelayedRepaintTask implements Runnable {

        @Nonnull private final RepaintTask mTask;

        DelayedRepaintTask(@Nonnull RepaintTask task) {
            mTask = task;
        }

        @Override
        public void run() {
            mTaskQueue.add(mTask);
        }
    }

    /**
     * Complements {@link #sExecutor} and adapts to {@link RepaintContext#repaint(MediaContent)}. We need to preserve the following points:
     * <ul>
     * <li>make all tasks posted into the {@link #sExecutor shared executor} to perform almost immediately;</li>
     * <li>don't allow race conditions during {@link RepaintContext#repaint(MediaContent) perform drawing};</li>
     * </ul>
     * So, our solution is to have tasks posted into the {@link #sExecutor shared executor} submit an instance of this class
     * into the {@link #mTaskQueue tasks queue}. That is fast and makes repainting to be performed from the same decoding thread.
     */
    private class RepaintTask extends AbstractTask {
        @Override
        public void run() {
            if (mRepaintTask == this) {
                repaint();
            }
        }
    }

    private class DecodeTask extends AbstractTask {
        @Override
        public void run() {
            if (!mRunning || mDecoder.isReleased()) {
                return;
            }
            if (!mInitialized) {
                Dimensions d = mDecoder.init();
                if (d == null) {
                    return;
                }
                mInitialized = true;
                mBitmap = Bitmap.createBitmap(d.getWidth(), d.getHeight(), Bitmap.Config.ARGB_8888);
                mBitmapRect = new Rect(0, 0, d.getWidth(), d.getHeight());
            }
            final long frameOffsetTimeMillis = mDecoder.fillNextFrame(mBitmap);
            onDecodeTaskCompleted(frameOffsetTimeMillis);
        }
    }
}
