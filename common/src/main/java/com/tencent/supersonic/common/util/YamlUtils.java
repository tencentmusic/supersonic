package com.tencent.supersonic.common.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;


public class YamlUtils {

    /**
     * 将yaml字符串转成类对象
     *
     * @param yamlStr 字符串
     * @param clazz 目标类
     * @param <T> 泛型
     * @return 目标类
     */
    public static <T> T toObject(String yamlStr, Class<T> clazz) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        try {
            return mapper.readValue(yamlStr, clazz);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将类对象转yaml字符串
     *
     * @param object 对象
     * @return yaml字符串
     */
    public static String toYaml(Object object) {
        YAMLMapper mapper = new YAMLMapper();
        mapper.findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES).disable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE);
        try {
            String yaml = mapper.writeValueAsString(object);
            return yaml.replaceAll("\"True\"", "true")
                    .replaceAll("\"true\"", "true")
                    .replaceAll("\"false\"", "false")
                    .replaceAll("\"False\"", "false");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String toYamlWithoutNull(Object object) {
        String jsonStr = JSONObject.toJSONString(object);
        if (object instanceof List) {
            return toYaml(JSONObject.parseObject(jsonStr, List.class, Feature.OrderedField));
        } else {
            return toYaml(JSONObject.parseObject(jsonStr, LinkedHashMap.class, Feature.OrderedField));
        }


    }

    /**
     * （此方法非必要）
     * json 2 yaml
     *
     * @param jsonStr json
     * @return yaml
     * @throws JsonProcessingException Exception
     */
    public static String json2Yaml(String jsonStr) throws JsonProcessingException {
        JsonNode jsonNode = new ObjectMapper().readTree(jsonStr);
        return new YAMLMapper().writeValueAsString(jsonNode);
    }

}
