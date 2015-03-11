package bo.pic.android.media.bitmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public interface BitmapPool {
    public Bitmap get(BitmapFactory.Options options);

    public boolean put(Bitmap bitmap);
}
