package bo.pic.android.media.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import javax.annotation.Nonnull;

import bo.pic.android.media.bitmap.BitmapPool;

public class ImageUtil {

    public static boolean isMp4(@Nonnull byte[] data) {
                // 33 67 70 35
        return (data.length >= 4 && data[0] == '3' && data[1] == 'g' && data[2] == 'p' && data[3] == '5') ||
               //00 00 00 nn 66 74 79 70
               (data.length >= 8 && data[4] == 'f' && data[5] == 't' && data[6] == 'y' && data[7] == 'p');
    }

    @Nonnull
    public static Bitmap decodeBitmap(byte[] data,
                                      int desiredWidth,
                                      int desiredHeight,
                                      @Nonnull ScaleMode scaleMode,
                                      @Nonnull Bitmap.Config config,
                                      @Nonnull BitmapPool bitmapPool)
    {
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapUtil.decodeByteArray(data, desiredWidth, desiredHeight, scaleMode, decodeOptions, config);

        addInBitmapOptions(decodeOptions, bitmapPool);
        return bitmap;
    }

    private static void addInBitmapOptions(BitmapFactory.Options options, BitmapPool bitmapPool) {
        // inBitmap only works with mutable bitmaps
        options.inMutable = true;

        if (bitmapPool != null) {
            final Bitmap inBitmap = bitmapPool.get(options);
            if (inBitmap != null) {
                options.inBitmap = inBitmap;
            }
        }
    }

    /**
     * Image processing is organized as follows:
     * <ul>
     *   <li>every image is identifies by its URI;</li>
     *   <li>
     *       there is a set of pre-generated image flavors for the original image as well (they correspond to various
     *       {@link ImageUri.Mode}). E.g. there is a {@link ImageUri.Mode#PRESERVE_ASPECT_ANIMATED_90 scaled to height 90} mode;
     *   </li>
     * </ul>
     * This method takes an image URI, checks if it points not to original image but to one of its flavors and derives original image
     * URI if possible.
     *
     * @param uri    an image URI to process
     * @return       original image's URI if given URI was recognized to point to one of original image flavors;
     *               given URI instead
     */
    @Nonnull
    public static String getBaseUri(@Nonnull String uri) {
        for (ImageUri.Mode mode : ImageUri.Mode.values()) {
            String postfix = mode.getPostfix();
            if (!postfix.isEmpty() && uri.endsWith(postfix)) {
                return uri.substring(0, uri.length() - postfix.length());
            }
        }
        return uri;
    }
}
