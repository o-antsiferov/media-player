package bo.pic.android.media.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nullable;

public final class FileUtil {
    private FileUtil() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public static File getFile(final @Nullable String url) {
        if (url == null) {
            return null;
        }

        final String path = Scheme.FILE.foundIn(url) ? Scheme.FILE.unwrap(url) : url;
        final File file = new File(path);

        return file.isFile() ? file : null;
    }

    public static void write(final byte[] bytes, final File file) throws IOException {
        final OutputStream stream = new FileOutputStream(file, false);

        try {
            stream.write(bytes);
        } finally {
           IoUtil.closeQuietly(stream);
        }
    }

    public static void move(File from, File to) throws IOException {
        if (!from.renameTo(to)) {
            final InputStream input = new FileInputStream(from);
            final OutputStream output = new FileOutputStream(to);

            IoUtil.copy(input, output);

            if (!from.delete()) {
                if (!to.delete()) {
                    throw new IOException("Unable to delete " + to);
                }
                throw new IOException("Unable to delete " + from);
            }
        }
    }
}
