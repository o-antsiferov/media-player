package bo.pic.android.media.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import bo.pic.android.media.util.DeviceUtil;

public class SoftReferenceBitmapPool implements BitmapPool {
    private final Set<SoftReference<Bitmap>> mReusableBitmaps;

    public SoftReferenceBitmapPool() {
        mReusableBitmaps = Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());
    }

    @Override
    public boolean put(Bitmap bitmap) {
        return mReusableBitmaps.add(new SoftReference<>(bitmap));
    }

    @Override
    public Bitmap get(BitmapFactory.Options options) {
        Bitmap bitmap = null;
        if (!mReusableBitmaps.isEmpty()) {
            synchronized (mReusableBitmaps) {
                final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps.iterator();
                Bitmap item;
                while (iterator.hasNext()) {
                    item = iterator.next().get();
                    if (null != item && item.isMutable()) {
                        // Check to see it the item can be used for inBitmap
                        if (canUseForInBitmap(item, options)) {
                            bitmap = item;
                            // Remove from reusable set so it can't be used again
                            iterator.remove();
                            break;
                        }
                    } else {
                        // Remove from the set if the reference has been cleared.
                        iterator.remove();
                    }
                }
            }
        }

        return bitmap;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static boolean canUseForInBitmap(
            Bitmap candidate, BitmapFactory.Options targetOptions) {

        if (!DeviceUtil.hasKitKat()) {
            // On earlier versions, the dimensions must match exactly and the inSampleSize must be 1
            return candidate.getWidth() == targetOptions.outWidth
                    && candidate.getHeight() == targetOptions.outHeight
                    && targetOptions.inSampleSize == 1;
        }

        // From Android 4.4 (KitKat) onward we can re-use if the byte size of the new bitmap
        // is smaller than the reusable bitmap candidate allocation byte count.
        int width = targetOptions.outWidth / (targetOptions.inSampleSize == 0 ? 1 : targetOptions.inSampleSize);
        int height = targetOptions.outHeight / (targetOptions.inSampleSize == 0 ? 1 : targetOptions.inSampleSize);
        int byteCount = width * height * getBytesPerPixel(candidate.getConfig());
        return byteCount <= candidate.getAllocationByteCount();
    }

    private static int getBytesPerPixel(Bitmap.Config config) {
        switch (config) {
            case ARGB_8888:
                return 4;
            case RGB_565:
                return 2;
            case ARGB_4444:
                return 2;
            case ALPHA_8:
                return 1;
            default:
                return 1;
        }
    }
}