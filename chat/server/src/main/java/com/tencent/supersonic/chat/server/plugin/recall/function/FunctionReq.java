package com.tencent.supersonic.chat.server.plugin.recall.function;

import com.tencent.supersonic.chat.server.plugin.PluginParseConfig;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FunctionReq {

    private String queryText;

    private List<PluginParseConfig> pluginConfigs;

}
