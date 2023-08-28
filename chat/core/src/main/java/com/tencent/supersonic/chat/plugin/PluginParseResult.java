package com.tencent.supersonic.chat.plugin;

import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import lombok.Data;

@Data
public class PluginParseResult {

    private Plugin plugin;
    private QueryReq request;
    private double distance;
}
