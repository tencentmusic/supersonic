package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;

@Data
public class PluginQueryReq {

    private String name;

    private String parseMode;

    private String type;

    private String dataSet;

    private String pattern;

    private String createdBy;
}
