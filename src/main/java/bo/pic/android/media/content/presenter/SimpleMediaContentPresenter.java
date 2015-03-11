package bo.pic.android.media.content.presenter;

import javax.annotation.Nonnull;

import bo.pic.android.media.content.MediaContent;
import bo.pic.android.media.view.MediaContentView;

public class SimpleMediaContentPresenter implements MediaContentPresenter {
    @Override
    public void setMediaContent(@Nonnull MediaContent content, @Nonnull MediaContentView view) {
        view.setMediaContent(content, true);
    }
}