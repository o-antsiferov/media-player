package bo.pic.android.media.content.presenter;

import javax.annotation.Nonnull;

import bo.pic.android.media.content.MediaContent;
import bo.pic.android.media.view.MediaContentView;

public interface MediaContentPresenter {

    /**
     * Sets {@link bo.pic.android.media.content.MediaContent media content} to a {@link MediaContentView media content view}. Always called from UI thread.
     *
     * @param content Media content to set.
     * @param view    Media content view where content is set.
     */
    void setMediaContent(@Nonnull MediaContent content, @Nonnull MediaContentView view);
}
