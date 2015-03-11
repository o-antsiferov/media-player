package bo.pic.android.media.util;

public enum ScaleMode {
    /**
     * Scales the image the minimum amount (maintaining aspect ration) so that at least one of the two dimensions (width/height) fit
     * inside the requested destination area. Source may be cropped.
     */
    CROP,

    /**
     * Scales the image the minimum amount (maintaining aspect ration) so that both dimensions fit inside the requested destination
     * area.
     */
    FIT
}
