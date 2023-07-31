package com.tencent.supersonic.chat.query.plugin;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class WebBase {

    private String url;

    //key, id of schema element
    private Map<String, Object> params = new HashMap<>();

    //key, value of shcema element
    private Map<String, Object> valueParams = new HashMap<>();

    //only forward
    private Map<String, Object> forwardParam = new HashMap<>();

}
