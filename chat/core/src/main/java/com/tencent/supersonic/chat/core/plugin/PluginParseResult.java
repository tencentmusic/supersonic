package com.tencent.supersonic.chat.core.plugin;

import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import lombok.Data;

@Data
public class PluginParseResult {

    private Plugin plugin;
    private QueryFilters queryFilters;
    private double distance;
}
