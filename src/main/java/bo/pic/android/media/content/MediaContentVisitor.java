package bo.pic.android.media.content;

import javax.annotation.Nonnull;

import bo.pic.android.media.content.animation.AnimatedImageContent;

public interface MediaContentVisitor {

    void visit(@Nonnull StaticImageContent content);
    void visit(@Nonnull AnimatedImageContent content);

    abstract class Adapter implements MediaContentVisitor {
        @Override
        public void visit(@Nonnull StaticImageContent content) {
        }

        @Override
        public void visit(@Nonnull AnimatedImageContent content) {
        }
    }
}
