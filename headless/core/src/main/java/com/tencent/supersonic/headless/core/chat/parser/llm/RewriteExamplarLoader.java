package com.tencent.supersonic.headless.core.chat.parser.llm;


import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Component
public class RewriteExamplarLoader {

    private static final String EXAMPLE_JSON_FILE = "rewrite_examplar.json";

    private TypeReference<List<RewriteExample>> valueTypeRef = new TypeReference<List<RewriteExample>>() {
    };

    public List<RewriteExample> getRewriteExamples() {
        try {
            ClassPathResource resource = new ClassPathResource(EXAMPLE_JSON_FILE);
            InputStream inputStream = resource.getInputStream();
            return JsonUtil.INSTANCE.getObjectMapper().readValue(inputStream, valueTypeRef);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
