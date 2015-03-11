package bo.pic.android.media;

import java.io.Serializable;

public class Dimensions implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int mWidth;
    private final int mHeight;

    public Dimensions(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    @Override
    public int hashCode() {
        int result = mWidth;
        result = 31 * result + mHeight;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dimensions that = (Dimensions) o;
        return mHeight == that.mHeight && mWidth == that.mWidth;
    }

    @Override
    public String toString() {
        return mWidth + "x" + mHeight;
    }
}
