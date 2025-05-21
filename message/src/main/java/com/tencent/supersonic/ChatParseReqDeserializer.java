package com.tencent.supersonic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class ChatParseReqDeserializer implements Deserializer<ChatParseReq> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}

    @Override
    public ChatParseReq deserialize(String s, byte[] data) {
        if (Objects.isNull(data)) {
            return null;
        }
        try {
            return objectMapper.readValue(data, ChatParseReq.class);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void close() {}
}
