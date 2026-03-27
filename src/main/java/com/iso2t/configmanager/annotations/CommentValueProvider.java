package com.iso2t.configmanager.annotations;

import java.lang.reflect.Field;

/**
 * Produces comment lines dynamically for a config field at serialisation time.
 *
 * @param <T> the expected type of the field's default value
 */
public interface CommentValueProvider<T> {
    /**
     * Return the lines that should be emitted above the field.
     *
     * @param field        the config field being serialised
     * @param defaultValue the current (default) value of the field
     * @return zero or more comment lines (without the leading {@code // })
     */
    String[] getCommentLines (Field field, T defaultValue);
}
