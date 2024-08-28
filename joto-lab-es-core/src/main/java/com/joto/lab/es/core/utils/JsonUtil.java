package com.joto.lab.es.core.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.util.List;


/**
 * @author joey
 * @date 2023/7/12 11:05
 */
public final class JsonUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    private JsonUtil() {
    }

    /**
     * 转换成 json
     *
     * @param data data
     * @param <T>  T
     * @return json str
     * @throws JsonProcessingException jpe
     */
    public static <T> String toJsonStr(T data) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(data);
    }


    /**
     * json 转换成对象
     *
     * @param data  data
     * @param clazz clazz
     * @param <T>   T
     * @return clazz obj
     * @throws JsonProcessingException jpe
     */
    public static <T> T str2Obj(String data, Class<T> clazz) throws IOException {
        return OBJECT_MAPPER.readValue(data, clazz);
    }

    public static <T> T str2Obj(String data, TypeReference<T> typeReference) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(data, typeReference);
    }

    /**
     * 转换成对象
     *
     * @param obj   obj
     * @param clazz clazz
     * @param <T>   T
     * @return clazz obj
     */
    public static <T> T toObject(Object obj, Class<T> clazz) {
        return OBJECT_MAPPER.convertValue(obj, clazz);
    }

    public static <T> T toObject(String data, TypeReference<T> typeReference) {
        return OBJECT_MAPPER.convertValue(data, typeReference);
    }

    public static <T> List<T> strToList(String data, Class<T> clazz) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(data, getCollectionType(List.class, clazz));
    }

    private static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
        return OBJECT_MAPPER.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }
}
