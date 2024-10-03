package com.tencent.supersonic.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class JsonUtil {

    public static final JsonUtil INSTANCE = new JsonUtil();

    @Getter
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonUtil() {
        // 当属性为null时不参与序列化
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 允许使用未带引号的字段名
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        // 忽略未知enum字段，置为null
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        // 反序列化忽略未知字段
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 允许使用单引号
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        // 遇到空对象不抛异常
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 遇到未知属性时不会抛一个JsonMappingException
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 允许JSON整数以多个0开始
        objectMapper.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
        // Java8日期时间类支持
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * jsonNode转object
     *
     * @param jsonNode jsonNode
     * @param clazz 被转对象的class
     * @param <T> 通配符
     * @return string
     */
    public static <T> T toObject(JsonNode jsonNode, Class<T> clazz) {
        return INSTANCE.asObject(jsonNode, clazz);
    }

    /**
     * json转object
     *
     * @param json json字符串
     * @param clazz 被转对象的类型
     * @param <T> 通配符
     * @return string
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        return INSTANCE.asObject(json, clazz);
    }

    /**
     * json转object
     *
     * @param json json字符串
     * @param wrapperClass 包装类对象类型
     * @param typeClass 类型类对象类型
     * @param <W> 包装类通配符
     * @param <T> 类型类通配符
     * @return string
     */
    public static <T, W> W toObject(String json, Class<W> wrapperClass, Class<T> typeClass) {
        return INSTANCE.asObject(json, wrapperClass, typeClass);
    }

    /**
     * 通过typeReference和json转对象
     *
     * @param json json串
     * @param typeReference 类型
     * @param <T> 通配符
     * @return 对象
     */
    public static <T> T toObject(String json, TypeReference<T> typeReference) {
        return INSTANCE.asObject(json, typeReference);
    }

    /**
     * bytes转对象
     *
     * @param json json二进制数组
     * @param clazz 被转对象的类型
     * @param <T> 通配符
     * @return string
     */
    public static <T> T toObject(byte[] json, Class<T> clazz) {
        return INSTANCE.asObject(json, clazz);
    }

    /**
     * json转list
     *
     * @param json json字符串
     * @param clazz 被转对象的类
     * @param <T> 被转对象的类型
     * @return 列表
     */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        return INSTANCE.asList(json, clazz);
    }

    /**
     * json转set
     *
     * @param json json字符串
     * @param clazz 被转对象的类型
     * @param <T> 通配符
     * @return string
     */
    public static <T> Set<T> toSet(String json, Class<T> clazz) {
        return INSTANCE.asSet(json, clazz);
    }

    /**
     * json转map
     *
     * @param json json字符串
     * @param keyClass key类型
     * @param valueClass value类型
     * @param <K> 键类型
     * @param <V> 值类型
     * @return map
     */
    public static <K, V> Map<K, V> toMap(String json, Class<K> keyClass, Class<V> valueClass) {
        return INSTANCE.asMap(json, keyClass, valueClass);
    }

    public static Map<String, Object> objectToMap(Object obj) {
        return INSTANCE.asobjectToMap(obj);
    }

    public static Map<String, String> objectToMapString(Object obj) {
        return INSTANCE.asObjectToMapString(obj);
    }

    public static <V> List<Map<String, V>> jsonToListMap(String json, Class<V> clazz) {
        return INSTANCE.asjsonToListMap(json, clazz);
    }

    public static <T> T mapToObject(Map<?, ?> map, Class<T> clazz) {
        return INSTANCE.asmapToObject(map, clazz);
    }

    /**
     * 对象转json
     *
     * @param object 被转换的对象
     * @return 对象的json字符串
     */
    public static String toString(Object object) {
        return INSTANCE.asString(object);
    }

    public static String toString(Object object, boolean pretty) {
        return INSTANCE.asString(object, pretty);
    }

    /**
     * 对象转带缩进的json
     *
     * @param object 被转换的对象
     * @return 对象的json字符串
     */
    public static String prettyToString(Object object) {
        return INSTANCE.prettyAsString(object);
    }

    /**
     * json转jsonNode
     *
     * @param json json字符串
     * @return jsonnode
     */
    public static JsonNode readTree(String json) {
        return INSTANCE.asTree(json);
    }

    /**
     * clone一个新的对象
     *
     * @param object 被克隆对象
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 新的对象
     */
    public static <T> T clone(T object, Class<T> clazz) {
        return INSTANCE.asClone(object, clazz);
    }

    /**
     * 对象转objectNode
     *
     * @param object 对象
     * @return objectNode
     */
    public static ObjectNode getNode(Object object) {
        return INSTANCE.asNode(object);
    }

    /**
     * 判断字符串格式是否合法
     *
     * @param json
     * @return
     */
    public static boolean isJson(String json) {
        return INSTANCE.isJsonValid(json);
    }

    /**
     * jsonNode转object
     *
     * @param jsonNode jsonNode
     * @param clazz 被转对象的class
     * @param <T> 通配符
     * @return string
     */
    public <T> T asObject(JsonNode jsonNode, Class<T> clazz) {
        if (jsonNode == null) {
            return null;
        }
        try {
            notNull(clazz, "class is null");
            return objectMapper.treeToValue(jsonNode, clazz);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * json转object
     *
     * @param json json字符串
     * @param clazz 被转对象的类型
     * @param <T> 通配符
     * @return string
     */
    public <T> T asObject(String json, Class<T> clazz) {
        try {
            if (StringUtils.isBlank(json)) {
                return null;
            }
            notNull(clazz, "class is null");
            if (clazz == String.class) {
                return (T) json;
            }
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * json转object
     *
     * @param json json字符串
     * @param wrapperClass 包装类对象类型
     * @param typeClass 类型类对象类型
     * @param <W> 包装类通配符
     * @param <T> 类型类通配符
     * @return string
     */
    public <T, W> W asObject(String json, Class<W> wrapperClass, Class<T> typeClass) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            notNull(wrapperClass, "wrapperClass is null");
            notNull(typeClass, "typeClass is null");
            JavaType type =
                    objectMapper.getTypeFactory().constructParametricType(wrapperClass, typeClass);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 通过typeReference和json转对象
     *
     * @param json json串
     * @param typeReference 类型
     * @param <T> 通配符
     * @return 对象
     */
    public <T> T asObject(String json, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            notNull(typeReference, "typeReference is null");
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * bytes转对象
     *
     * @param bytes json二进制数组
     * @param clazz 被转对象的类型
     * @param <T> 通配符
     * @return string
     */
    public <T> T asObject(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        return asObject(new String(bytes, Charset.forName("utf-8")), clazz);
    }

    /**
     * json转list
     *
     * @param json json字符串
     * @param clazz 被转对象的类
     * @param <T> 被转对象的类型
     * @return 列表
     */
    public <T> List<T> asList(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            notNull(clazz, "class is null");
            JavaType type =
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * json转set
     *
     * @param json json字符串
     * @param clazz 被转对象的类型
     * @param <T> 通配符
     * @return string
     */
    public <T> Set<T> asSet(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            notNull(clazz, "class is null");
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(Set.class, clazz);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * json转map
     *
     * @param json json字符串
     * @param keyClass key类型
     * @param valueClass value类型
     * @param <K> 键类型
     * @param <V> 值类型
     * @return map
     */
    public <K, V> Map<K, V> asMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            notNull(keyClass, "key class is null");
            notNull(valueClass, "value class is null");
            JavaType type = objectMapper.getTypeFactory().constructParametricType(Map.class,
                    keyClass, valueClass);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /** 对象转换成Map */
    public Map<String, Object> asobjectToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
    }

    /** 对象转换成Map */
    public Map<String, String> asObjectToMapString(Object obj) {
        if (obj == null) {
            return null;
        }
        return objectMapper.convertValue(obj, new TypeReference<Map<String, String>>() {});
    }

    /**
     * map转换成bean对象
     *
     * @param map map对象
     * @param clazz bean对象类型
     * @param <T> 泛型
     * @return bean对象
     */
    public <T> T asmapToObject(Map<?, ?> map, Class<T> clazz) {
        if (map == null) {
            return null;
        }
        return objectMapper.convertValue(map, clazz);
    }

    /**
     * Json转换成ListMap
     *
     * @param json 序列化字符串
     * @param <V> map value泛型
     * @return 反序列化对象
     */
    public <V> List<Map<String, V>> asjsonToListMap(String json, Class<V> clazz) {
        try {
            final TypeFactory typeFactory = getObjectMapper().getTypeFactory();
            final MapType mapType = typeFactory.constructMapType(Map.class, String.class, clazz);
            final CollectionType collectionType =
                    typeFactory.constructCollectionType(List.class, mapType);
            return getObjectMapper().readValue(json, collectionType);
        } catch (Exception e) {
            log.error("json:{} to listMap error:", json, e);
            return null;
        }
    }

    /**
     * 对象转json
     *
     * @param object 被转换的对象
     * @return 对象的json字符串
     */
    public String asString(Object object) {
        if (object == null) {
            return "";
        }
        if (object instanceof String) {
            return (String) object;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    public String asString(Object object, boolean pretty) {
        return pretty ? prettyAsString(object) : asString(object);
    }

    /**
     * 对象转带缩进的json
     *
     * @param object 被转换的对象
     * @return 对象的json字符串
     */
    public String prettyAsString(Object object) {
        if (object == null) {
            return "";
        }
        try {
            if (object instanceof String) {
                String string = (String) object;
                if (!string.startsWith("{") || !string.endsWith("}")) {
                    return string;
                }
                try {
                    JsonNode jsonNode = readTree(string);
                    return objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(jsonNode);
                } catch (Exception e) {
                    return string;
                }
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * json转jsonNode
     *
     * @param json json字符串
     * @return jsonnode
     */
    public JsonNode asTree(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * clone一个新的对象
     *
     * @param object 被克隆对象
     * @param clazz 目标类型
     * @param <T> 类型参数
     * @return 新的对象
     */
    public <T> T asClone(T object, Class<T> clazz) {
        if (object == null) {
            return null;
        }
        try {
            notNull(clazz, "class is null");
            String json = toString(object);
            return toObject(json, clazz);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * * 判断字符串是否合格的json格式
     *
     * @param json
     * @return
     */
    public boolean isJsonValid(String json) {
        if (StringUtils.isBlank(json)) {
            return false;
        }
        try {
            objectMapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 对象转objectNode
     *
     * @param object 对象
     * @return objectNode
     */
    public ObjectNode asNode(Object object) {
        if (object == null) {
            return null;
        }
        return objectMapper.convertValue(object, ObjectNode.class);
    }

    /**
     * 判断某个参数是否为空
     *
     * @param object 参数
     * @param description 参数描述
     */
    public static void notNull(Object object, String description) {
        if (object == null) {
            description = StringUtils.isBlank(description) ? "参数" : description;
            throw new InvalidParameterException(description + "为空");
        }
    }

    /** json序列化,或者反序列化发生的异常 */
    public static class JsonException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private JsonException() {}

        private JsonException(String message) {
            super(message);
        }

        private JsonException(String message, Throwable cause) {
            super(message, cause);
        }

        private JsonException(Throwable cause) {
            super(cause);
        }

        private JsonException(String message, Throwable cause, boolean enableSuppression,
                boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }
}
