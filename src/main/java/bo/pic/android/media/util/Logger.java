package bo.pic.android.media.util;

import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import bo.pic.android.media.BuildConfig;

public class Logger {
    private static Map<Class<?>, String> sLogTagsMap = new HashMap<Class<?>, String>();

    private Logger() {
    }

    private static String getLogTag(Class<?> cls) {
        if (!sLogTagsMap.containsKey(cls)) {
            sLogTagsMap.put(cls, makeLogTag(cls));
        }
        return sLogTagsMap.get(cls);
    }

    private static String makeLogTag(Class<?> cls) {
        return BuildConfig.LOG_PREFIX + "." + cls.getSimpleName();
    }

    public static void v(Class<?> cls, String format, Object... args) {
        logIfEnabled(Log.VERBOSE, cls, format, null, args);
    }

    public static void v(Class<?> cls, String format, Throwable throwable, Object... args) {
        logIfEnabled(Log.VERBOSE, cls, format, throwable, args);
    }

    public static void d(Class<?> cls, String format, Object... args) {
        logIfEnabled(Log.DEBUG, cls, format, null, args);
    }

    public static void d(Class<?> cls, String format, Throwable throwable, Object... args) {
        logIfEnabled(Log.DEBUG, cls, format, throwable, args);
    }

    public static void i(Class<?> cls, String format, Object... args) {
        logIfEnabled(Log.INFO, cls, format, null, args);
    }

    public static void i(Class<?> cls, String format, Throwable throwable, Object... args) {
        logIfEnabled(Log.INFO, cls, format, throwable, args);
    }

    public static void w(Class<?> cls, String format, Object... args) {
        logIfEnabled(Log.WARN, cls, format, null, args);
    }

    public static void w(Class<?> cls, String format, Throwable throwable, Object... args) {
        logIfEnabled(Log.WARN, cls, format, throwable, args);
    }

    public static void e(Class<?> cls, String format, Object... args) {
        logIfEnabled(Log.ERROR, cls, format, null, args);
    }

    public static void e(Class<?> cls, String format, Throwable throwable, Object... args) {
        logIfEnabled(Log.ERROR, cls, format, throwable, args);
    }

    private static void logIfEnabled(int level, Class<?> cls, String format, Throwable throwable, Object... args) {
        if (BuildConfig.LOG_ENABLED && BuildConfig.LOG_LEVEL <= level) {
            switch (level) {
                case Log.VERBOSE:
                    Log.v(getLogTag(cls), buildMessage(format, args), throwable);
                    break;
                case Log.INFO:
                    Log.i(getLogTag(cls), buildMessage(format, args), throwable);
                    break;
                case Log.DEBUG:
                    Log.d(getLogTag(cls), buildMessage(format, args), throwable);
                    break;
                case Log.WARN:
                    Log.w(getLogTag(cls), buildMessage(format, args), throwable);
                    break;
                case Log.ERROR:
                    Log.e(getLogTag(cls), buildMessage(format, args), throwable);
                    break;
            }
        }
    }

    private static String buildMessage(String format, Object... args) {
        return (args == null || args.length == 0) ? format : String.format(Locale.US, format, args);
    }
}
