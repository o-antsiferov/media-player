package bo.pic.android.media;

import javax.annotation.Nonnull;

/**
 * Represents possible media content types.
 * <p/>
 * The general idea is that we might want to process differently contents of different types. E.g. small avatars (used at chat messages)
 * must be kept in memory cache all the time etc.
 */
public class MediaContentType {
    @Nonnull private final String mType;

    public MediaContentType(@Nonnull String type) {
        mType = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MediaContentType that = (MediaContentType) o;
        return mType.equals(that.mType);

    }

    @Override
    public int hashCode() {
        return mType.hashCode();
    }

    @Override
    public String toString() {
        return mType;
    }
}