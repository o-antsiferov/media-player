package bo.pic.android.media.content;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

public abstract class AbstractMediaContent implements MediaContent {

    private final Set<RepaintContext> mContexts         = Collections.newSetFromMap(new ConcurrentHashMap<RepaintContext, Boolean>());
    private final Set<RepaintContext> mActiveContexts   = Collections.newSetFromMap(new ConcurrentHashMap<RepaintContext, Boolean>());
    private final AtomicInteger       mReferenceCounter = new AtomicInteger();

    @Nonnull private final String mContentUri;

    protected AbstractMediaContent(@Nonnull String contentUri) {
        mContentUri = contentUri;
    }

    @Nonnull
    @Override
    public String getContentUri() {
        return mContentUri;
    }

    @Override
    public boolean addRepaintContext(@Nonnull RepaintContext context) {
        boolean added = mContexts.add(context);
        if (added) {
            incrementUsageCounter();
        }
        return added;
    }

    @Override
    public boolean removeRepaintContext(@Nonnull RepaintContext context) {
        stopDrawingFor(context);
        boolean removed = mContexts.remove(context);
        if (removed) {
            decrementUsageCounter();
        }
        return removed;
    }

    @Override
    public boolean isDrawingEnabledFor(@Nonnull RepaintContext context) {
        return mActiveContexts.contains(context);
    }

    @Override
    public void startDrawingFor(@Nonnull RepaintContext context) {
        if (!mContexts.contains(context)) {
            throw new IllegalStateException(
                    String.format("Can not start drawing for an unregistered previously context {%s}", context));
        }
        doStartDrawingFor(context);
    }

    protected abstract void doStartDrawingFor(@Nonnull RepaintContext context);

    @Override
    public void stopDrawingFor(@Nonnull RepaintContext context) {
        boolean removed = mActiveContexts.remove(context);
        if (removed && mActiveContexts.isEmpty()) {
            doStopDrawing();
        }
    }

    protected abstract void doStopDrawing();

    @Nonnull
    protected Collection<RepaintContext> getActiveContexts() {
        return mActiveContexts;
    }

    @Override
    public void incrementUsageCounter() {
        mReferenceCounter.incrementAndGet();
    }

    @Override
    public void decrementUsageCounter() {
        int refCount = mReferenceCounter.get();
        while (true) {
            if (refCount <= 0) {
                throw new IllegalStateException("Unbalanced decrementReference() call for media content " + this);
            }
            if (mReferenceCounter.compareAndSet(refCount, --refCount)) {
                break;
            }
            refCount = mReferenceCounter.get();
        }
        if (refCount == 0) {
            release();
        }
    }
}
