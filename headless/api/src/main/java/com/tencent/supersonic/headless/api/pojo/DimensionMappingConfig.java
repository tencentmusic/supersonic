package com.tencent.supersonic.headless.api.pojo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
public class DimensionMappingConfig {

    @Value("${s2.dimension.mapping.pid1_2022}")
    private String pid1;

    @Value("${s2.dimension.mapping.pid2_2022}")
    private String pid2;

    @Value("${s2.dimension.mapping.pid3_2022}")
    private String pid3;

    @Value("${s2.dimension.mapping.pid4_2022}")
    private String pid4;

    private static final Map<String, String> mapping = new HashMap<>();

    @PostConstruct
    public void init() {
        mapping.put("pid1_2022", pid1);
        mapping.put("pid2_2022", pid2);
        mapping.put("pid3_2022", pid3);
        mapping.put("pid4_2022", pid4);
    }

    public static Map<String, String> getMapping() {
        return mapping;
    }
}


