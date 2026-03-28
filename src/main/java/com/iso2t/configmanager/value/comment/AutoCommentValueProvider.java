package com.iso2t.configmanager.value.comment;

import com.iso2t.configmanager.annotations.CommentValueProvider;
import com.iso2t.configmanager.annotations.CommentValues;

import java.lang.reflect.Field;

/**
 * A marker class used by {@link CommentValues} to indicate that the
 * {@link CommentValueProvider} should be automatically detected based on the
 * field's type.
 *
 * <p>This provider does not produce any lines itself; it is only a marker.
 */
public final class AutoCommentValueProvider implements CommentValueProvider<Object> {
    @Override
    public String[] getCommentLines (Field field, Object defaultValue) {
        return new String[0];
    }
}
