package bo.pic.android.media.content;

/**
 * Stands for an entity which usage counter is externally updated and, when it's decreased to zero, {@link #release()}
 * is automatically called.
 */
public interface AutoReleasable {

    void incrementUsageCounter();

    /**
     * Decrements usage counter for the current object.
     * <p/>
     * {@link #release()} must be automatically called when the counter reaches zero.
     */
    void decrementUsageCounter();

    void release();
}
