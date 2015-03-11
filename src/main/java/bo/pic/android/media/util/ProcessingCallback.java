package bo.pic.android.media.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ProcessingCallback<T> {

    void onSuccess(@Nonnull T data);

    void onFail(@Nullable Throwable e);
}
