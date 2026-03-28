package com.iso2t.configmanager.value.comment;

import com.iso2t.configmanager.annotations.CommentValueProvider;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Built-in {@link CommentValueProvider} for enum-backed config fields.
 *
 * <p>Emits two lines:
 * <ol>
 *   <li>{@code Default: <constantName>}</li>
 *   <li>{@code Allowed values: <CONST1>, <CONST2>, …}</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>{@code
 * @CommentValues
 * private Mode mode = Mode.BALANCED;
 * }</pre>
 */
public final class EnumValues implements CommentValueProvider<Enum<?>> {

    @Override
    public String[] getCommentLines (Field field, Enum<?> defaultValue) {
        Class<?> type = field.getType();
        // If the field is a wrapper (e.g. EnumValue<Mode>) rather than a raw enum,
        // derive the enum class from the default value's runtime type.
        if (!type.isEnum() && defaultValue != null) {
            type = defaultValue.getClass();
        }
        Object[] constants = type.getEnumConstants();
        if (constants == null) {
            throw new IllegalArgumentException(
                    "@CommentValues(EnumValues.class) requires an enum field, but field '"
                            + field.getName() + "' has type " + type.getName());
        }

        String allowed = Arrays.stream(constants)
                .map(Object::toString)
                .collect(Collectors.joining(", "));

        return new String[] {
                "Default: " + defaultValue.name(),
                "Allowed values: " + allowed
        };
    }
}
