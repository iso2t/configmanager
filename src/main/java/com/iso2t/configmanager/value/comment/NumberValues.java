package com.iso2t.configmanager.value.comment;

import com.iso2t.configmanager.annotations.CommentValueProvider;
import com.iso2t.configmanager.value.ConfigValue;
import com.iso2t.configmanager.value.NumberRange;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Built-in {@link CommentValueProvider} for {@link NumberRange}-backed config fields.
 *
 * <p>Emits three lines:
 * <ol>
 *   <li>{@code Default: <currentValue>} (if the field implements {@link ConfigValue})</li>
 *   <li>{@code Min: <minValue>}</li>
 *   <li>{@code Max: <maxValue>}</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>{@code
 * @CommentValues
 * private IntegerValue port = new IntegerValue(8080, 1024, 65535);
 * }</pre>
 */
public final class NumberValues implements CommentValueProvider<NumberRange<?>> {

    @Override
    public String[] getCommentLines(Field field, NumberRange<?> range) {
        if (range == null) return new String[0];

        List<String> lines = new ArrayList<>();

        if (range instanceof ConfigValue<?> cv) {
            lines.add("Default: " + cv.get());
        }

        lines.add("Min: " + range.getMin() + " | Max: " + range.getMax());

        return lines.toArray(new String[0]);
    }
}
