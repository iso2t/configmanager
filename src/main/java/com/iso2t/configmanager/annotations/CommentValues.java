package com.iso2t.configmanager.annotations;

import com.iso2t.configmanager.value.comment.AutoCommentValueProvider;
import com.iso2t.configmanager.value.comment.EnumValues;
import com.iso2t.configmanager.value.comment.NumberValues;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates comment lines above a config field at serialization time by
 * delegating to a {@link CommentValueProvider} implementation.
 *
 * <p>When both {@code @Comment} and {@code @CommentValues} are present on the
 * same field, the manual {@code @Comment} lines are emitted first, followed by
 * the lines produced by the provider.
 *
 * <p>Example:
 * <pre>{@code
 * @CommentValues
 * private Mode mode = Mode.BALANCED;
 *
 * @CommentValues
 * private IntegerValue port = new IntegerValue(8080, 1024, 65535);
 * }</pre>
 *
 * <p>By default, the implementation is automatically detected based on the
 * field type. For enum-like fields, {@link EnumValues} is used. For
 * {@link com.iso2t.configmanager.value.NumberRange} fields,
 * {@link NumberValues} is used.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CommentValues {
    /**
     * The {@link CommentValueProvider} implementation to use for this field.
     * Use {@link EnumValues} for enums and {@link NumberValues} for numbers.
     * If left as default, the provider will be automatically detected.
     */
    Class<? extends CommentValueProvider<?>> value () default AutoCommentValueProvider.class;
}
