package com.iso2t.configmanager.manager;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.iso2t.configmanager.annotations.AutoComment;
import com.iso2t.configmanager.annotations.Comment;
import com.iso2t.configmanager.annotations.CommentValueProvider;
import com.iso2t.configmanager.annotations.Config;
import com.iso2t.configmanager.value.AbstractValue;
import com.iso2t.configmanager.value.ConfigValue;
import com.iso2t.configmanager.value.wrappers.ListValue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ConfigManager<T> {
    private final Class<T> type;
    private final Path file;
    private final ObjectMapper mapper;

    public ConfigManager (Class<T> type, Path file) {
        this.type = type;
        this.file = file;

        JsonFactory factory = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                .build();

        this.mapper = new ObjectMapper(factory)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Load (or create) the config instance
     */
    public T load () throws IOException, IllegalAccessException {
        T cfg = instantiate(type);
        if (Files.exists(file)) {
            JsonNode root = mapper.readTree(Files.newBufferedReader(file));
            populate(cfg, root);
        }
        return cfg;
    }

    /**
     * Write out with comments
     */
    public void save (T config) throws IOException, IllegalAccessException {
        Path parent = file.getParent();
        if (parent != null && Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter w = Files.newBufferedWriter(file,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            writeObject(config, w, 0);
        }
    }

    private <U> U instantiate (Class<U> cls) {
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate " + cls.getName(), e);
        }
    }

    private void populate (Object obj, JsonNode node) throws IOException, IllegalAccessException {
        for (Field f : obj.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            String key = f.getName().toLowerCase();
            JsonNode child = node.get(key);

            if (f.getType().isAnnotationPresent(Config.class)) {
                populateNestedConfig(obj, f, child);
                continue;
            }

            if (ListValue.class.isAssignableFrom(f.getType())) {
                populateListValue(obj, f, child);
                continue;
            }

            if (ConfigValue.class.isAssignableFrom(f.getType())) {
                populateScalarValue(obj, f, child);
                continue;
            }

            if (child != null && !child.isNull()) {
                f.set(obj, mapper.treeToValue(child, f.getType()));
            }
        }
    }

    private void populateNestedConfig (Object obj, Field f, JsonNode child) throws IllegalAccessException, IOException {
        Object nested = f.get(obj);
        if (nested == null) {
            nested = instantiate(f.getType());
            f.set(obj, nested);
        }
        if (child != null && child.isObject()) {
            populate(nested, child);
        }
    }

    private void populateListValue (Object obj, Field f, JsonNode child) throws IOException, IllegalAccessException {
        if (child == null || !child.isArray()) return;

        Type gt = f.getGenericType();
        if (!(gt instanceof ParameterizedType pt)) {
            throw new IOException("Missing generic type for ListValue on field " + f.getName());
        }
        Type arg = pt.getActualTypeArguments()[0];
        if (!(arg instanceof Class<?> declaredElem)) {
            throw new IOException("Cannot handle generic type " + arg + " on field " + f.getName());
        }

        Class<?> elemType = declaredElem;
        if (ConfigValue.class.isAssignableFrom(declaredElem)) {
            elemType = unwrapValueType(declaredElem);
        }

        List<Object> built = new ArrayList<>();
        for (JsonNode elNode : child) {
            built.add(parseListElement(elNode, elemType));
        }

        Object raw = f.get(obj);
        if (raw instanceof ListValue<?> lv) {
            @SuppressWarnings("unchecked")
            ListValue<Object> listVal = (ListValue<Object>) lv;
            listVal.set(built);
        } else {
            throw new IllegalStateException("Field " + f.getName() + " is not a ListValue: " + raw.getClass());
        }
    }

    private Object parseListElement (JsonNode elNode, Class<?> elemType) throws IOException, IllegalAccessException {
        if (elemType.isAnnotationPresent(Config.class)) {
            Object element = instantiate(elemType);
            populate(element, elNode);
            return element;
        }

        if (elNode.isValueNode()) {
            return mapper.treeToValue(elNode, elemType);
        } else if (elNode.isObject()) {
            JsonNode valNode = elNode.get("value");
            if (valNode != null && valNode.isValueNode()) {
                return mapper.treeToValue(valNode, elemType);
            } else {
                return mapper.convertValue(elNode, elemType);
            }
        } else {
            return mapper.convertValue(elNode, elemType);
        }
    }

    private void populateScalarValue (Object obj, Field f, JsonNode child) throws IllegalAccessException, IOException {
        @SuppressWarnings("unchecked")
        ConfigValue<Object> cv = (ConfigValue<Object>) f.get(obj);
        if (child != null && !child.isNull()) {
            Object v = mapper.treeToValue(child, inferGenericType(f));
            cv.set(v);
        }
    }

    private Class<?> inferGenericType (Field f) {
        Type gt = f.getGenericType();
        if (gt instanceof ParameterizedType pt) {
            Type[] args = pt.getActualTypeArguments();
            if (args.length == 1 && args[0] instanceof Class<?> argClass) {
                return argClass;
            }
        }

        return switch (f.getType().getSimpleName()) {
            case "BooleanValue" -> Boolean.class;
            case "IntegerValue" -> Integer.class;
            case "StringValue" -> String.class;
            case "DoubleValue" -> Double.class;
            case "LongValue" -> Long.class;
            case "FloatValue" -> Float.class;
            case "ShortValue" -> Short.class;
            case "ByteValue" -> Byte.class;
            default -> throw new IllegalStateException("Unknown ConfigValue type " + f.getType().getSimpleName());
        };
    }

    private void writeObject (Object obj, BufferedWriter w, int indent) throws IOException, IllegalAccessException {
        indent(w, indent);
        w.write("{");
        w.newLine();

        Field[] fields = obj.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            f.setAccessible(true);
            String key = f.getName().toLowerCase();

            writeComments(f, obj, w, indent);

            if (f.getType().isAnnotationPresent(Config.class)) {
                writeNestedConfig(obj, f, key, w, indent);
            } else if (ConfigValue.class.isAssignableFrom(f.getType())) {
                writeConfigValue(obj, f, key, w, indent);
            } else {
                writePlainField(obj, f, key, w, indent);
            }

            if (i < fields.length - 1) w.write(",");
            w.newLine();
        }

        indent(w, indent);
        w.write("}");
        if (indent == 0) w.newLine();
    }

    private void writeComments (Field f, Object obj, BufferedWriter w, int indent) throws IOException {
        Comment comment = f.getAnnotation(Comment.class);
        if (comment != null) {
            for (String line : comment.value()) {
                indent(w, indent + 1);
                w.write("// " + line);
                w.newLine();
            }
        }

        AutoComment autoComment = f.getAnnotation(AutoComment.class);
        if (autoComment != null) {
            try {
                @SuppressWarnings("unchecked")
                CommentValueProvider<Object> provider =
                        (CommentValueProvider<Object>) autoComment.value().getDeclaredConstructor().newInstance();
                Object fieldValue = f.get(obj);
                Object currentValue = fieldValue instanceof ConfigValue<?> cv ? cv.get() : fieldValue;
                for (String line : provider.getCommentLines(f, currentValue)) {
                    indent(w, indent + 1);
                    w.write("// " + line);
                    w.newLine();
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "Failed to invoke AutoComment provider " + autoComment.value().getName(), e);
            }
        }
    }

    private void writeNestedConfig (Object obj, Field f, String key, BufferedWriter w, int indent) throws IllegalAccessException, IOException {
        Object nested = f.get(obj);
        if (nested == null) {
            nested = instantiate(f.getType());
            f.set(obj, nested);
        }

        indent(w, indent + 1);
        w.write(key + ": ");
        writeObject(nested, w, indent + 1);
    }

    private void writeConfigValue (Object obj, Field f, String key, BufferedWriter w, int indent) throws IllegalAccessException, IOException {
        @SuppressWarnings("unchecked")
        ConfigValue<Object> cv = (ConfigValue<Object>) f.get(obj);
        Object val = cv.get();

        indent(w, indent + 1);
        w.write(key + ": ");

        if (val instanceof Collection<?> coll) {
            writeCollection(coll, w, indent);
        } else {
            w.write(mapper.writeValueAsString(val));
        }
    }

    private void writePlainField (Object obj, Field f, String key, BufferedWriter w, int indent) throws IllegalAccessException, IOException {
        Object val = f.get(obj);
        indent(w, indent + 1);
        w.write(key + ": ");
        w.write(mapper.writeValueAsString(val));
    }

    private void writeCollection (Collection<?> coll, BufferedWriter w, int indent) throws IOException, IllegalAccessException {
        w.write("[");
        if (!coll.isEmpty()) {
            w.newLine();
            Iterator<?> it = coll.iterator();
            while (it.hasNext()) {
                Object element = it.next();
                indent(w, indent + 2);

                if (element != null && element.getClass().isAnnotationPresent(Config.class)) {
                    writeObject(element, w, indent + 2);
                } else {
                    w.write(mapper.writeValueAsString(element));
                }

                if (it.hasNext()) w.write(",");
                w.newLine();
            }
            indent(w, indent + 1);
        }
        w.write("]");
    }

    private void indent (BufferedWriter w, int levels) throws IOException {
        for (int i = 0; i < levels; i++) w.write("    ");
    }

    private Class<?> unwrapValueType (Class<?> wrapper) {
        Type sup = wrapper.getGenericSuperclass();
        if (sup instanceof ParameterizedType pt && pt.getRawType() == AbstractValue.class) {
            Type t = pt.getActualTypeArguments()[0];
            if (t instanceof Class<?> c) return c;
        }
        throw new IllegalStateException("Cannot unwrap wrapper type " + wrapper);
    }
}
