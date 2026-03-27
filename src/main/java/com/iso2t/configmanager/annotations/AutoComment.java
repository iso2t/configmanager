package com.iso2t.configmanager.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates comment lines above a config field at serialisation time by
 * delegating to a {@link CommentValueProvider} implementation.
 *
 * <p>When both {@code @Comment} and {@code @AutoComment} are present on the
 * same field, the manual {@code @Comment} lines are emitted first, followed by
 * the lines produced by the provider.
 *
 * <p>Example:
 * <pre>{@code
 * @AutoComment(EnumValues.class)
 * private Mode mode = Mode.BALANCED;
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoComment {
    /**
     * The {@link CommentValueProvider} implementation to use for this field.
     */
    Class<? extends CommentValueProvider<?>> value ();
}
