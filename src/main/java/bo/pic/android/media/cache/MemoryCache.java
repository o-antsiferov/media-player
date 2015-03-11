package bo.pic.android.media.cache;

import javax.annotation.Nonnull;

public interface MemoryCache<K, V> extends Cache<K, V> {

    interface RemoveFromCacheListener<V> {
        void onRemoved(V value);
    }

    void setRemoveFromCacheListener(@Nonnull RemoveFromCacheListener<V> listener);
}