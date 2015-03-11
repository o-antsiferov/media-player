package bo.pic.android.media.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import javax.annotation.Nonnull;

public class BitmapUtil {

    @Nonnull
    public static Bitmap decodeByteArray(@Nonnull byte[] data,
                                         int desiredWidth,
                                         int desiredHeight,
                                         @Nonnull ScaleMode scaleMode,
                                         @Nonnull BitmapFactory.Options decodeOptions,
                                         @Nonnull Bitmap.Config config)
    {
        Bitmap bitmap;
        if (desiredWidth == 0 && desiredHeight == 0) {
            decodeOptions.inPreferredConfig = config;
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        } else {
            // If we need to resize this image, first get the natural bounds.
            decodeOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            int actualWidth = decodeOptions.outWidth;
            int actualHeight = decodeOptions.outHeight;

            // Decode to the nearest power of two scaling factor.
            decodeOptions.inJustDecodeBounds = false;
            decodeOptions.inSampleSize = calculateSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight, scaleMode);
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);

            Bitmap result = createScaledBitmap(bitmap, desiredWidth, desiredHeight, scaleMode);
            if (result != bitmap) {
                bitmap.recycle();
                bitmap = result;
            }
        }
        return bitmap;
    }

    /**
     * Calculates down-sampling factor as the power of two (sample size) given the dimensions of a source, the desired dimensions and a
     * {@link ScaleMode scale mode}. This sample size is used at {@link android.graphics.BitmapFactory.Options bitmap options} during decoding bitmap.
     *
     * @param sourceWidth   Width of source image
     * @param sourceHeight  Height of source image
     * @param desiredWidth  Width of destination area
     * @param desiredHeight Height of destination area
     * @param scaleMode     Scale mode
     *
     * @return Optimal downscaling sample size for decoding
     */
    public static int calculateSampleSize(int sourceWidth,
                                          int sourceHeight,
                                          int desiredWidth,
                                          int desiredHeight,
                                          @Nonnull ScaleMode scaleMode)
    {
        float wr = (float) sourceWidth / desiredWidth;
        float hr = (float) sourceHeight / desiredHeight;
        float ratio;
        switch (scaleMode) {
            case FIT:
                ratio = Math.min(wr, hr);
                break;
            case CROP:
                ratio = Math.max(wr, hr);
                break;
            default:
                throw new IllegalArgumentException("Invalid scale mode {" + scaleMode + "}");
        }
        float scale = 1f;
        while ((scale * 2) <= ratio) {
            scale *= 2;
        }
        return (int) scale;
    }

    @Nonnull
    public static Bitmap createScaledBitmap(@Nonnull Bitmap sourceBitmap,
                                            int desiredWidth,
                                            int desiredHeight,
                                            @Nonnull ScaleMode scaleMode)
    {
        int bw = sourceBitmap.getWidth();
        int bh = sourceBitmap.getHeight();
        if (bw == desiredWidth && bh == desiredHeight) {
            return sourceBitmap;
        }

        RectPair rectPair = RectPair.calculate(sourceBitmap.getWidth(), sourceBitmap.getHeight(), desiredWidth, desiredHeight, scaleMode);
        Bitmap scaledBitmap = Bitmap.createBitmap(rectPair.dstRect.width(), rectPair.dstRect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.drawBitmap(sourceBitmap, rectPair.srcRect, rectPair.dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));
        return scaledBitmap;
    }

    private static class RectPair {
        final Rect srcRect = new Rect();
        final Rect dstRect = new Rect();

        @Nonnull
        public static RectPair calculate(int srcWidth, int srcHeight, int dstWidth, int dstHeight, @Nonnull ScaleMode scaleMode) {
            RectPair rectPair = new RectPair();
            boolean srcAspectIsBigger = srcWidth * dstHeight > srcHeight * dstWidth;

            switch (scaleMode) {
                case CROP:
                    // calculate source rect
                    float dstAspect = (float) dstWidth / dstHeight;
                    if (srcAspectIsBigger) {
                        int srcRectWidth = (int) (srcHeight * dstAspect);
                        int srcRectLeft = (srcWidth - srcRectWidth) / 2;
                        rectPair.srcRect.set(srcRectLeft, 0, srcRectLeft + srcRectWidth, srcHeight);
                    } else {
                        int srcRectHeight = (int) (srcWidth / dstAspect);
                        int scrRectTop = (srcHeight - srcRectHeight) / 2;
                        rectPair.srcRect.set(0, scrRectTop, srcWidth, scrRectTop + srcRectHeight);
                    }

                    // calculate destination rect
                    rectPair.dstRect.set(0, 0, dstWidth, dstHeight);
                    break;
                case FIT:
                    // calculate source rect
                    rectPair.srcRect.set(0, 0, srcWidth, srcHeight);

                    // calculate destination rect
                    float srcAspect = (float) srcWidth / srcHeight;
                    if (srcAspectIsBigger) {
                        rectPair.dstRect.set(0, 0, dstWidth, (int) (dstWidth / srcAspect));
                    } else {
                        rectPair.dstRect.set(0, 0, (int) (dstHeight * srcAspect), dstHeight);
                    }
                    break;
            }
            return rectPair;
        }
    }
}
