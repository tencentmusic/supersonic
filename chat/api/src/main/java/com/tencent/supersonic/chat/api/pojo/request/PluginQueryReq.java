package com.tencent.supersonic.chat.api.pojo.request;


import lombok.Data;

@Data
public class PluginQueryReq {


    private String showElementId;

    //DASHBOARD WIDGET
    private String showType;

    private String type;

    private String domain;

    private String pattern;


}
