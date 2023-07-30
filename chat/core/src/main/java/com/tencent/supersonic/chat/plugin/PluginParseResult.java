package com.tencent.supersonic.chat.plugin;

import com.tencent.supersonic.chat.api.pojo.request.QueryRequest;
import lombok.Data;

@Data
public class PluginParseResult {

    private Plugin plugin;
    private QueryRequest request;
}
