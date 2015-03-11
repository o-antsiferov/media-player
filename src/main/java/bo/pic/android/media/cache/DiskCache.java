package bo.pic.android.media.cache;

import java.io.File;

import javax.annotation.Nonnull;

public interface DiskCache<K> extends Cache<K, byte[]> {

    @Nonnull
    File getFile(K key);
}
