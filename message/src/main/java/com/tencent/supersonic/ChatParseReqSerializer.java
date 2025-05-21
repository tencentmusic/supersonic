package com.tencent.supersonic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class ChatParseReqSerializer implements Serializer<ChatParseReq> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Serializer.super.configure(configs, isKey);
    }

    @Override
    public byte[] serialize(String topic, ChatParseReq chatParseReq) {
        // 这是GPT给出的可能性能不高的自动序列化器。这里如果自己递归地写序列化方法会非常麻烦
        // objectMapper.writeValueAsBytes()方法在处理带有isSuperAdmin()这种类型的方法识别为一个为“superAdmin”的field，而这种自动
        // 识别所带来的反序列化发现异常的“superAdmin”会带来bug。解决办法是在原类的isSuperAdmin()方法上加
        // @com.fasterxml.jackson.annotation.JsonIgnore 注解
        try {
            return objectMapper.writeValueAsBytes(chatParseReq);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void close() {}
}