package com.tencent.supersonic.chat.api.pojo.response;

import java.util.Map;
import lombok.Data;

@Data
public class MetricInfo {

    private String name;
    private String dimension;
    private String value;
    private String date;
    private Map<String, String> statistics;

}
