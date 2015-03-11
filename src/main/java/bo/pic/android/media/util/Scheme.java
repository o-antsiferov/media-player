package bo.pic.android.media.util;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents supported schemes of {@link android.net.Uri}.
 * Provides methods for wrapping/unwrapping uris with corresponding scheme.
 */
public enum Scheme {
    HTTP("http"), HTTPS("https"), FILE("file"), CONTENT("content"), EMPTY("");

    @Nonnull private String mScheme;
    @Nonnull private String mSchemePrefix;

    private Scheme(@Nonnull String scheme) {
        mScheme = scheme;
        mSchemePrefix = scheme + "://";
    }

    @Nonnull
    public static Scheme of(@Nullable String uri) {
        if (uri != null) {
            for (Scheme s : values()) {
                if (s.foundIn(uri)) {
                    return s;
                }
            }
        }
        return EMPTY;
    }

    public boolean foundIn(@Nonnull String uri) {
        return uri.toLowerCase(Locale.US).startsWith(mSchemePrefix);
    }

    @Nonnull
    public String wrap(@Nonnull String path) {
        return mSchemePrefix + path;
    }

    @Nonnull
    public String unwrap(@Nonnull String uri) {
        if (!foundIn(uri)) {
            throw new IllegalArgumentException(String.format("Uri {%1$s} doesn't contain expected scheme {%2$s}", uri, mScheme));
        }
        return uri.substring(mSchemePrefix.length());
    }
}