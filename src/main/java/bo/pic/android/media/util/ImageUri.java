package bo.pic.android.media.util;

import java.io.Serializable;
import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ImageUri contains URI to image with extension and original image size.
 * <p/>
 * This uri can be relative or absolute.
 * If relative, it should be converted to absolute URI using application-configured base.
 * <p/>
 * Given URI with extension is URI of full-sized image on server.
 * To generate URI of specific image size client should add size-specific part to the end of filename.
 */
public class ImageUri implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final  String  ANIMATED_EXTENSION   = ".mp4";
    private static final Pattern VALID_IMAGE_ID       = Pattern.compile("[-~a-zA-Z0-9.:/]+");
    private static final Pattern STRING_PARSE_PATTERN = Pattern.compile("([-~a-zA-Z0-9.:/]+)\\|([0-9]+)x([0-9]+)");
    private final String id;
    private final int    width;
    private final int    height;

    public ImageUri(@Nonnull String id, int width, int height) {
        if (!VALID_IMAGE_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Illegal image id: " + id);
        }
        this.id = id;
        this.width = width;
        this.height = height;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isAnimated() {
        return id.endsWith(ANIMATED_EXTENSION);
    }

    /**
     * Similar to {@link #toUri(URI, Mode)} but doesn't create new {@link URI} object (android profiling shows that that
     * new {@link URI} object construction occupies 8% of the CPU time during active message typing).
     *
     * @param serviceAddress    target service address
     * @param mode              target content mode
     * @return                  uri which points to the current {@link ImageUri} content at the given service and mode
     */
    @Nonnull
    public String toUriAsString(@Nonnull URI serviceAddress, @Nonnull Mode mode) {
        if (!mode.imageExists(height)) {
            throw new IllegalArgumentException("There is no URI for mode " + mode + " for image " + this);
        }
        if (id.indexOf("://") > 0) {
            return id + mode.getPostfix();
        } else {
            return serviceAddress.toString() + id + mode.getPostfix();
        }
    }

    @Nonnull
    public URI toUri(@Nonnull URI serviceAddress, @Nonnull Mode mode) {
        String uri = toUriAsString(serviceAddress, mode);
        return URI.create(uri);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s|%dx%d", id, width, height);
    }

    @Nonnull
    public static ImageUri fromString(@Nonnull String input) {
        Matcher matcher = STRING_PARSE_PATTERN.matcher(input);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Can not parse string to ImageUri: " + input);
        }
        return new ImageUri(matcher.group(1), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageUri imageUri = (ImageUri) o;

        if (height != imageUri.height) return false;
        if (width != imageUri.width) return false;
        if (!id.equals(imageUri.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }

    public enum Mode {
        NORMAL(AspectRatioMode.ORIGINAL_SIZE, null, true),
        NORMAL_NOT_ANIMATED(AspectRatioMode.ORIGINAL_SIZE, null, false),
        NORMAL_SQUARE(AspectRatioMode.ORIGINAL_SIZE_SQUARE, null, true),
        NORMAL_SQUARE_NOT_ANIMATED(AspectRatioMode.ORIGINAL_SIZE_SQUARE, null, false),

        SQUARE_NOT_ANIMATED_60(AspectRatioMode.SQUARE, 60, false),
        SQUARE_NOT_ANIMATED_120(AspectRatioMode.SQUARE, 120, false),
        SQUARE_NOT_ANIMATED_212(AspectRatioMode.SQUARE, 212, false),
        SQUARE_NOT_ANIMATED_140(AspectRatioMode.SQUARE, 140, false),
        SQUARE_NOT_ANIMATED_106(AspectRatioMode.SQUARE, 106, false),
        SQUARE_NOT_ANIMATED_70(AspectRatioMode.SQUARE, 70, false),
        SQUARE_NOT_ANIMATED_160(AspectRatioMode.SQUARE, 160, false),
        SQUARE_NOT_ANIMATED_320(AspectRatioMode.SQUARE, 320, false),

        SQUARE_ANIMATED_60(AspectRatioMode.SQUARE, 60, true),
        SQUARE_ANIMATED_120(AspectRatioMode.SQUARE, 120, true),
        SQUARE_ANIMATED_212(AspectRatioMode.SQUARE, 212, true),
        SQUARE_ANIMATED_140(AspectRatioMode.SQUARE, 140, true),
        SQUARE_ANIMATED_106(AspectRatioMode.SQUARE, 106, true),
        SQUARE_ANIMATED_70(AspectRatioMode.SQUARE, 70, true),

        PRESERVE_ASPECT_NOT_ANIMATED_90(AspectRatioMode.PRESERVE_ASPECT_RATIO, 90, false),
        PRESERVE_ASPECT_NOT_ANIMATED_180(AspectRatioMode.PRESERVE_ASPECT_RATIO, 180, false),
        PRESERVE_ASPECT_NOT_ANIMATED_360(AspectRatioMode.PRESERVE_ASPECT_RATIO, 360, false),
        PRESERVE_ASPECT_NOT_ANIMATED_240(AspectRatioMode.PRESERVE_ASPECT_RATIO, 240, false),
        PRESERVE_ASPECT_NOT_ANIMATED_120(AspectRatioMode.PRESERVE_ASPECT_RATIO, 120, false),
        PRESERVE_ASPECT_NOT_ANIMATED_540(AspectRatioMode.PRESERVE_ASPECT_RATIO, 540, false),
        PRESERVE_ASPECT_NOT_ANIMATED_270(AspectRatioMode.PRESERVE_ASPECT_RATIO, 270, false),

        PRESERVE_ASPECT_ANIMATED_90(AspectRatioMode.PRESERVE_ASPECT_RATIO, 90, true),
        PRESERVE_ASPECT_ANIMATED_180(AspectRatioMode.PRESERVE_ASPECT_RATIO, 180, true),
        PRESERVE_ASPECT_ANIMATED_360(AspectRatioMode.PRESERVE_ASPECT_RATIO, 360, true),
        PRESERVE_ASPECT_ANIMATED_240(AspectRatioMode.PRESERVE_ASPECT_RATIO, 240, true),
        PRESERVE_ASPECT_ANIMATED_120(AspectRatioMode.PRESERVE_ASPECT_RATIO, 120, true),
        PRESERVE_ASPECT_ANIMATED_320(AspectRatioMode.PRESERVE_ASPECT_RATIO, 320, true),
        PRESERVE_ASPECT_ANIMATED_640(AspectRatioMode.PRESERVE_ASPECT_RATIO, 640, true);

        private final String          postfix;
        private final AspectRatioMode aspectRatioMode;
        private final Integer         requestedHeight;
        private final boolean         canBeAnimated;

        Mode(@Nonnull AspectRatioMode aspectRatioMode, @Nullable Integer requestedHeight, boolean canBeAnimated) {
            this.aspectRatioMode = aspectRatioMode;
            this.requestedHeight = requestedHeight;
            this.canBeAnimated = canBeAnimated;
            this.postfix = postfix(aspectRatioMode, requestedHeight, canBeAnimated);
        }

        private static String postfix(@Nonnull AspectRatioMode aspectRatioMode, @Nullable Integer requestedHeight, boolean canBeAnimated) {
            switch (aspectRatioMode) {
                case ORIGINAL_SIZE:
                    assert requestedHeight == null;
                    return canBeAnimated ? "" : ".jpg";
                case ORIGINAL_SIZE_SQUARE:
                    assert requestedHeight == null;
                    return canBeAnimated ? ".s" : ".s.jpg";
                default:
                    return "." +
                            requestedHeight +
                            (aspectRatioMode == AspectRatioMode.PRESERVE_ASPECT_RATIO ? "h" : "s") +
                            (canBeAnimated ? "" : ".jpg");
            }
        }

        @Nonnull
        public String getPostfix() {
            return postfix;
        }

        @Nonnull
        public AspectRatioMode getAspectRatioMode() {
            return aspectRatioMode;
        }

        public int getRequestedHeight() {
            if (requestedHeight == null) {
                throw new UnsupportedOperationException("Requested height is unsupported for " + name()); /*Should be fired on NORMAL*/
            }
            return requestedHeight;
        }

        public float getFloatRequestedHeight() {
            if (requestedHeight == null) {
                throw new UnsupportedOperationException("Requested height is unsupported for " + name()); /*Should be fired on NORMAL*/
            }
            return requestedHeight;
        }

        public int calculateWidth(int originalWidth, int originalHeight) {
            switch (aspectRatioMode) {
                case ORIGINAL_SIZE:
                    return originalWidth;
                case ORIGINAL_SIZE_SQUARE:
                    return Math.min(originalWidth, originalHeight);
                case PRESERVE_ASPECT_RATIO:
                    return (int) (originalWidth * (getFloatRequestedHeight() / originalHeight));
                case SQUARE:
                    return getRequestedHeight();
            }
            throw new UnsupportedOperationException("Unknown aspectRatioMode: " + aspectRatioMode);
        }

        /**
         * Checks if image exists based on its {@link AspectRatioMode aspect ratio mode} and requested height compared with
         * original image height.
         *
         * @param originalHeight The original image height.
         * @return {@code true} if image exists.
         */
        public boolean imageExists(int originalHeight) {
            return imageExists(Integer.MAX_VALUE, originalHeight);
        }

        /**
         * Checks if image exists based on its {@link AspectRatioMode aspect ratio mode} and requested height compared with
         * original image width and height.
         *
         * @param originalWidth The original image width.
         * @param originalHeight The original image height.
         * @return {@code true} if image exists.
         */
        public boolean imageExists(int originalWidth, int originalHeight) {
            return aspectRatioMode == AspectRatioMode.ORIGINAL_SIZE
                    || aspectRatioMode == AspectRatioMode.ORIGINAL_SIZE_SQUARE
                    || (getRequestedHeight() <= originalWidth && getRequestedHeight() <= originalHeight);
        }

        public boolean canBeAnimated() {
            return canBeAnimated;
        }

        @Override
        public String toString() {
            return postfix;
        }
    }

    public enum AspectRatioMode {
        ORIGINAL_SIZE, ORIGINAL_SIZE_SQUARE, PRESERVE_ASPECT_RATIO, SQUARE
    }
}