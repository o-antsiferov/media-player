package bo.pic.android.media.cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface Cache<K, V> {

    @Nullable
    V put(@Nonnull K key, @Nonnull V value);

    @Nullable
    V get(@Nonnull K key);

    @Nullable
    V remove(@Nonnull K key);

    void clear();
}
