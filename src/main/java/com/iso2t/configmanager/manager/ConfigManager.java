package com.iso2t.configmanager.manager;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.iso2t.configmanager.annotations.*;
import com.iso2t.configmanager.value.AbstractValue;
import com.iso2t.configmanager.value.ConfigValue;
import com.iso2t.configmanager.value.NumberRange;
import com.iso2t.configmanager.value.comment.AutoCommentValueProvider;
import com.iso2t.configmanager.value.comment.EnumValues;
import com.iso2t.configmanager.value.comment.NumberValues;
import com.iso2t.configmanager.value.wrappers.EnumValue;
import com.iso2t.configmanager.value.wrappers.ListValue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
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

            if (isNestedConfig(f.getType())) {
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
        if (isNestedConfig(elemType)) {
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

        String typeName = f.getType().getSimpleName();
        return switch (typeName) {
            case "BooleanValue" -> Boolean.class;
            case "IntegerValue" -> Integer.class;
            case "StringValue" -> String.class;
            case "DoubleValue" -> Double.class;
            case "LongValue" -> Long.class;
            case "FloatValue" -> Float.class;
            case "ShortValue" -> Short.class;
            case "ByteValue" -> Byte.class;
            case "ObjectValue" -> Object.class;
            default -> throw new IllegalStateException("Unknown ConfigValue type " + typeName);
        };
    }

    private void writeObject (Object obj, BufferedWriter w, int indent) throws IOException, IllegalAccessException {
        w.write("{");
        w.newLine();

        Field[] fields = obj.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            f.setAccessible(true);
            String key = f.getName().toLowerCase();

            writeComments(f, obj, w, indent);

            if (isNestedConfig(f.getType())) {
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

        CommentValues commentValues = f.getAnnotation(CommentValues.class);
        if (commentValues != null) {
            try {
                Class<? extends CommentValueProvider<?>> providerClass = commentValues.value();
                Object fieldValue = f.get(obj);

                if (providerClass == AutoCommentValueProvider.class) {
                    providerClass = detectProvider(f, fieldValue);
                }

                if (providerClass == null) return;

                @SuppressWarnings("unchecked")
                CommentValueProvider<Object> provider =
                        (CommentValueProvider<Object>) providerClass.getDeclaredConstructor().newInstance();
                Object currentValue = fieldValue instanceof ConfigValue<?> cv ? cv.get() : fieldValue;

                Object toPass = currentValue;
                if (fieldValue != null) {
                    Class<?> expectedType = getProviderExpectedType(providerClass);
                    // If the provider specifically expects the wrapper (e.g. NumberRange),
                    // but the currentValue is just a primitive/object (e.g. Integer), pass the wrapper.
                    if (expectedType.isInstance(fieldValue) && !expectedType.isInstance(currentValue)) {
                        toPass = fieldValue;
                    }
                }

                for (String line : provider.getCommentLines(f, toPass)) {
                    indent(w, indent + 1);
                    w.write("// " + line);
                    w.newLine();
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "Failed to invoke AutoComment provider " + commentValues.value().getName(), e);
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

                if (element != null && isNestedConfig(element.getClass())) {
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

    private boolean isNestedConfig (Class<?> cls) {
        if (cls == null) return false;
        if (cls.isAnnotationPresent(Config.class)) return true;
        if (cls.isPrimitive() || cls.isEnum() || cls.isArray()) return false;
        if (cls.getName().startsWith("java.") || cls.getName().startsWith("javax.")) return false;
        if (ConfigValue.class.isAssignableFrom(cls)) return false;
        if (Collection.class.isAssignableFrom(cls)) return false;
        try {
            cls.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private Class<?> unwrapValueType (Class<?> wrapper) {
        Type sup = wrapper.getGenericSuperclass();
        if (sup instanceof ParameterizedType pt && pt.getRawType() == AbstractValue.class) {
            Type t = pt.getActualTypeArguments()[0];
            if (t instanceof Class<?> c) return c;
        }
        throw new IllegalStateException("Cannot unwrap wrapper type " + wrapper);
    }

    private Class<? extends CommentValueProvider<?>> detectProvider (Field f, Object fieldValue) {
        Class<?> fieldType = f.getType();

        if (NumberRange.class.isAssignableFrom(fieldType)) return NumberValues.class;
        if (fieldValue instanceof NumberRange) return NumberValues.class;

        if (fieldType.isEnum()) return EnumValues.class;
        if (EnumValue.class.isAssignableFrom(fieldType)) return EnumValues.class;
        if (fieldValue instanceof Enum) return EnumValues.class;
        if (fieldValue instanceof EnumValue) return EnumValues.class;

        return null;
    }

    private Class<?> getProviderExpectedType (Class<?> cls) {
        for (Type t : cls.getGenericInterfaces()) {
            if (t instanceof ParameterizedType pt && pt.getRawType() == CommentValueProvider.class) {
                return extractClass(pt.getActualTypeArguments()[0]);
            }
        }
        Class<?> superCls = cls.getSuperclass();
        if (superCls != null && superCls != Object.class) {
            return getProviderExpectedType(superCls);
        }
        return Object.class;
    }

    private Class<?> extractClass (Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pt) return extractClass(pt.getRawType());
        if (type instanceof WildcardType wt) {
            Type[] bounds = wt.getUpperBounds();
            if (bounds.length > 0) return extractClass(bounds[0]);
        }
        return Object.class;
    }
}
