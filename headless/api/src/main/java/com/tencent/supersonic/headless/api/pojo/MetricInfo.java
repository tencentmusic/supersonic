package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

import java.util.Map;

@Data
public class MetricInfo {

    private String name;
    private String dimension;
    private String value;
    private String date;
    private Map<String, String> statistics;

}
