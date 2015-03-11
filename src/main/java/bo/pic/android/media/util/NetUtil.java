package bo.pic.android.media.util;

import javax.annotation.Nonnull;

public class NetUtil {
    @Nonnull
    public static String normalizeUri(@Nonnull String url) {
        return url.replaceAll("https", "http");
    }
}
