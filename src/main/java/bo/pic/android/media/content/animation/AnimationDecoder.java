package bo.pic.android.media.content.animation;

import android.graphics.Bitmap;
import android.os.Process;

import java.io.File;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import bo.pic.android.media.Dimensions;
import bo.pic.android.media.util.Logger;

public class AnimationDecoder {
    public interface Callback {

        /**
         * Notifies that the whole animation has been played and next call to {@link #fillNextFrame(android.graphics.Bitmap)} initializes the first
         * frame (the animation is looped).
         */
        void onDecoderReset();
    }

    private static class MyReference extends PhantomReference<AnimationDecoder> {

        @Nonnull private final AtomicLong mHandlePointer;
        @Nonnull private final AtomicBoolean mReleased;

        MyReference(@Nonnull AnimationDecoder decoder) {
            super(decoder, sReferenceQueue);
            mHandlePointer = decoder.mHandlePointer;
            mReleased = decoder.mReleased;
        }
    }

    private static final ReferenceQueue<AnimationDecoder> sReferenceQueue = new ReferenceQueue<>();
    private static final Set<MyReference> sRefs           = new HashSet<>();

    static {
        System.loadLibrary("avutil-52");
        System.loadLibrary("swscale-2");
        System.loadLibrary("avcodec-55");
        System.loadLibrary("avformat-55");
        System.loadLibrary("decoder");
    }

    static {
        Thread thread = new Thread() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                //noinspection InfiniteLoopStatement
                while (true) {
                    try {
                        MyReference ref = (MyReference) sReferenceQueue.remove();
                        sRefs.remove(ref);
                        if (ref.mReleased.compareAndSet(false, true)) {
                            long handle = ref.mHandlePointer.get();
                            if (handle != 0) { // There is a possible case that the decoder was created but not initialized
                                nativeRelease(handle);
                            }
                        }
                    } catch (InterruptedException e) {
                        Logger.e(AnimationDecoder.class, "Got an unexpected InterruptedException at the animation cleanup thread", e);
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        thread.start();
    }

    private final AtomicReference<WeakReference<Thread>> mThreadRef     = new AtomicReference<>();
    private final AtomicBoolean mReleased      = new AtomicBoolean();
    private final AtomicLong mHandlePointer = new AtomicLong();

    @Nonnull private final Callback mCallback;
    @Nonnull private final String mAbsoluteFilePath;

    public AnimationDecoder(@Nonnull File file, @Nonnull Callback listener) {
        this(file.getAbsolutePath(), listener);
    }

    /**
     * Creates a decoder with an absolute file path which will be used to find corresponding data to decode
     *
     * @param filePath Absolute file path
     */
    public AnimationDecoder(@Nonnull String filePath, @Nonnull Callback listener) {
        mAbsoluteFilePath = filePath;
        mCallback = listener;
        sRefs.add(new MyReference(this));
    }

    /**
     * It's a time consuming method. Consecutive calls to already initialized decoder do nothing.
     *
     * @return underlying animation's dimensions if initialization is ok;
     *         <code>null</code> otherwise
     */
    @Nullable
    public Dimensions init() {
        WeakReference<Thread> weakRef = new WeakReference<Thread>(Thread.currentThread());
        WeakReference<Thread> previousRef = mThreadRef.get();
        if (previousRef != null) {
            Thread previous = previousRef.get();
            if (previous != null) {
                throw new IllegalStateException(String.format("Detected attempt to perform double initialization of %s %d. Current "
                                + "thread: %s, previously initialized from %s",
                        getClass().getSimpleName(), System.identityHashCode(this),
                        Thread.currentThread(), previous));
            }
        }
        if (!mThreadRef.compareAndSet(previousRef, weakRef)) {
            previousRef = mThreadRef.get();
            Thread previousThread = previousRef == null ? null : previousRef.get();
            throw new IllegalStateException(String.format("Detected race condition on %s instance %s initialization. Current thread: %s, "
                            + "other thread: %s", getClass().getSimpleName(), System.identityHashCode(this),
                    Thread.currentThread(), previousThread));
        }
        try {
            mHandlePointer.set(nativeInit(mAbsoluteFilePath));
        } catch (Exception e) {
            Logger.w(AnimationDecoder.class, "Can't initialize animation decoder for file %s", mAbsoluteFilePath);
            mReleased.set(true);
            return null;
        }
        return getDimensions();
    }

    public boolean isReleased() {
        return mReleased.get() // Already released
               || (mHandlePointer.get() == 0 && !new File(mAbsoluteFilePath).isFile()); // Not initialized yet and can not be already
    }

    @Nonnull
    private Dimensions getDimensions() {
        int[] data = nativeGetDimensions(mHandlePointer.get());
        return new Dimensions(data[0], data[1]);
    }

    /**
     * @param bitmap    frame's data holder (is assumed to be initialized during this method processing)
     * @return          returned frame's time offset since the animation's beginning (in milliseconds)
     */
    public long fillNextFrame(@Nonnull Bitmap bitmap) {
        if (mReleased.get()) {
            return -1;
        }
        long result = nativeGetNextFrame(mHandlePointer.get(), bitmap);
        if (result < 0) {
            nativeReset(mHandlePointer.get());
            mCallback.onDecoderReset();
            result = nativeGetNextFrame(mHandlePointer.get(), bitmap);
        }
        return result;
    }

    public void reset() {
        if (mReleased.get()) {
            return;
        }
        nativeReset(mHandlePointer.get());
        mCallback.onDecoderReset();
    }

    public void release() {
        if (!mReleased.compareAndSet(false, true)) {
            Logger.w(AnimationDecoder.class, "release called second time for (%d)", System.identityHashCode(this));
            return;
        }
        long handle = mHandlePointer.get();
        if (handle != 0) { // There is a possible case that the decoder was created but not initialized
            nativeRelease(handle);
        }
    }

    private void checkState() {
        WeakReference<Thread> weakRef = mThreadRef.get();
        if (weakRef == null) {
            throw new IllegalStateException("Not possible to get a frame from uninitialized decoder");
        }
        Thread expected = weakRef.get();
        if (expected == null) {
            throw new IllegalStateException("Invalid state detected");
        }
        if (expected != Thread.currentThread()) {
            throw new IllegalStateException(String.format("Access to the current decoder (%s) is performed from an unexpected "
                            + "thread (%s). The right one is %s",
                    System.identityHashCode(this), Thread.currentThread(), expected));
        }
    }

    @Override
    public String toString() {
        return mAbsoluteFilePath + ", released: " + mReleased.get();
    }

    private static native long nativeInit(@Nonnull String absoluteFilePath);

    /**
     * Point decoder to the animation's beginning.
     *
     * @param handle    native processing data which should be used for all native decoder calls
     *                  (obtained initially via {@link #nativeInit(String)})
     */
    private static native void nativeReset(long handle);

    private static native long nativeGetNextFrame(long handle, Bitmap bitmap);

    private static native void nativeRelease(long handle);

    private static native int[] nativeGetDimensions(long handle);
}