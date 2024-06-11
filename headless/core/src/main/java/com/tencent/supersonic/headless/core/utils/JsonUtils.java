package com.tencent.supersonic.headless.core.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class JsonUtils {

    public static String toJsonString(Object object, SerializerFeature... features) {
        if (features.length != 0) {
            return JSON.toJSONString(object, features);
        } else {
            return JSON.toJSONString(object, SerializerFeature.WriteEnumUsingToString);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz, Feature... features) {
        if (features.length == 0) {
            return JSON.parseObject(json, clazz);
        } else {
            return JSON.parseObject(json, clazz, features);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> type, Feature... features) {
        if (features.length == 0) {
            return JSON.parseObject(json, type);
        } else {
            return JSON.parseObject(json, type, features);
        }
    }

}