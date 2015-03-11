package bo.pic.android.media.content.transformation;

import javax.annotation.Nonnull;

import bo.pic.android.media.content.MediaContent;

/**
 * Stands for a transformation that can be applied to a {@link MediaContent media content}
 */
public interface MediaContentTransformation {

    /**
     * Transforms the given content. It is also possible to return the given content if no transformation is required.
     *
     * @param content The content to transform
     *
     * @return The transformed content
     */
    @Nonnull MediaContent transform(@Nonnull MediaContent content);
}

