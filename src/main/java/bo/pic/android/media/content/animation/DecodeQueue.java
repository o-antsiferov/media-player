package bo.pic.android.media.content.animation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

public class DecodeQueue {
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        private static final int DECODE_THREAD_PRIORITY = Thread.NORM_PRIORITY - 1;

        @Nonnull
        public Thread newThread(final @Nonnull Runnable runnable) {
            final Thread thread = new Thread(runnable, "Decode thread #" + mCount.getAndIncrement());
            thread.setPriority(DECODE_THREAD_PRIORITY);
            thread.setDaemon(true);
            return thread;
        }
    };

    @Nonnull private final ExecutorService[] mExecutors;

    public DecodeQueue(int numDecodeThreads) {
        mExecutors = new ExecutorService[numDecodeThreads];
        for (int i = 0; i < numDecodeThreads; i++) {
            mExecutors[i] = Executors.newSingleThreadExecutor(sThreadFactory);
        }
    }

    public void add(final Task task) {
        mExecutors[task.getThreadId() % mExecutors.length].execute(task);
    }


    public interface Task extends Runnable {
        int getThreadId();
    }
}