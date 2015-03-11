package bo.pic.android.media.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class IoUtil {
    private IoUtil() {
        throw new UnsupportedOperationException();
    }

    public static BufferedInputStream toBufferedInputStream(final InputStream stream) throws IOException {
        if (stream instanceof BufferedInputStream) {
            return (BufferedInputStream) stream;
        }

        return new BufferedInputStream(stream);
    }

    public static BufferedOutputStream toBufferedOutputStream(final OutputStream stream) throws IOException{
        if (stream instanceof BufferedOutputStream) {
            return (BufferedOutputStream) stream;
        }

        return new BufferedOutputStream(stream);
    }

    public static ByteArrayOutputStream toByteArrayOutputStream(final InputStream stream) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(stream, output);
        return output;
    }

    public static byte[] toByteArray(final File file) throws IOException {
        final FileInputStream stream = new FileInputStream(file);

        try {
            return toByteArray(stream);
        } finally {
            closeQuietly(stream);
        }
    }

    public static String toString(final File file) throws IOException {
        final FileInputStream stream = new FileInputStream(file);

        try {
            return toString(stream);
        } finally {
            closeQuietly(stream);
        }
    }

    public static byte[] toByteArray(final InputStream stream) throws IOException {
        return toByteArrayOutputStream(stream).toByteArray();
    }

    public static String toString(final InputStream stream) throws IOException {
        return toByteArrayOutputStream(stream).toString("UTF-8");
    }

    public static ByteArrayInputStream toByteArrayInputStream(final String content) throws IOException {
        return toByteArrayInputStream(content.getBytes("UTF-8"));
    }

    public static ByteArrayInputStream toByteArrayInputStream(final byte[] content) throws IOException {
        return new ByteArrayInputStream(content);
    }

    public static void copy(final InputStream input, final OutputStream output) throws IOException {
        final BufferedInputStream buffered = new BufferedInputStream(input);
        final byte[] bytes = new byte[8 * 1024];

        while (true) {
            final int read = buffered.read(bytes);

            if (read <= 0) {
                break;
            }

            output.write(bytes, 0, read);
        }
    }

    public static void closeQuietly(final InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignore) {
                // close quietly
            }
        }
    }

    public static void closeQuietly(final OutputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignore) {
                // close quietly
            }
        }
    }
}
