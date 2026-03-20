package com.tencent.supersonic.chat.api.plugin;

import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import lombok.Data;

@Data
public class PluginParseResult {

    private ChatPlugin plugin;
    private QueryFilters queryFilters;
    private double distance;
    private String queryText;
    private Integer chatId;
    private Long queryId;
    private Long userId;
    private String userName;
    private Long tenantId;
}
