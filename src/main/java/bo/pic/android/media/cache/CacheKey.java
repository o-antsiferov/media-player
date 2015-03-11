package bo.pic.android.media.cache;

import javax.annotation.Nonnull;

import bo.pic.android.media.MediaContentType;

public class CacheKey<T> {
    @Nonnull public final T key;
    @Nonnull public final MediaContentType type;

    public CacheKey(@Nonnull T dataKey, @Nonnull MediaContentType type) {
        this.key = dataKey;
        this.type = type;
    }

    @Nonnull
    public static <T> CacheKey<T> of(@Nonnull T dataKey, @Nonnull MediaContentType type) {
        return new CacheKey<>(dataKey, type);
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        return 31 * result + type.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CacheKey key = (CacheKey) o;
        return type == key.type && this.key.equals(key.key);
    }

    @Override
    public String toString() {
        return type + ": " + key;
    }
}