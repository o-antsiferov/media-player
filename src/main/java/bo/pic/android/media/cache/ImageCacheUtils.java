package bo.pic.android.media.cache;

import javax.annotation.Nonnull;

import bo.pic.android.media.Dimensions;
import bo.pic.android.media.MediaContentType;

public final class ImageCacheUtils {

    private static final String UNDERSCORE_SEPARATOR       = "_";
    private static final String PROTOCOL_SEPARATOR         = "://";
    private static final String WIDTH_AND_HEIGHT_SEPARATOR = "x";

    private ImageCacheUtils() {
    }

    @Nonnull
    public static CacheKey<String> getMemoryCacheKey(@Nonnull String url,
                                                     @Nonnull MediaContentType type,
                                                     @Nonnull Dimensions dimensions)
    {
        String s = url + UNDERSCORE_SEPARATOR + dimensions.getWidth() + WIDTH_AND_HEIGHT_SEPARATOR + dimensions.getHeight();
        return CacheKey.of(s, type);
    }

    @Nonnull
    public static String getDiskCacheKey(@Nonnull String url) {
        int i = url.indexOf(PROTOCOL_SEPARATOR);
        if (i >= 0) {
            url = url.substring(i + PROTOCOL_SEPARATOR.length());
        }
        return url.replace('/', '-');
    }
}