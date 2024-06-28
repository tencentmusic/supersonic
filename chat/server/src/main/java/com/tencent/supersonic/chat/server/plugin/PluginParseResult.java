package com.tencent.supersonic.chat.server.plugin;

import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import lombok.Data;

@Data
public class PluginParseResult {

    private ChatPlugin plugin;
    private QueryFilters queryFilters;
    private double distance;
    private String queryText;
}
