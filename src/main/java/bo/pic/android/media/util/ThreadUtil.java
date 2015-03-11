package bo.pic.android.media.util;

import android.os.Handler;
import android.os.Looper;

public class ThreadUtil {
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    public static void runOnUiThread(final Runnable runnable) {
        if (Looper.myLooper() != sHandler.getLooper()) {
            sHandler.post(runnable);
        } else {
            runnable.run();
        }
    }
}
