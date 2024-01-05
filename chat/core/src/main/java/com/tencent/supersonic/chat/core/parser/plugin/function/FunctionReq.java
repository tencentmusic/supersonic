package com.tencent.supersonic.chat.core.parser.plugin.function;

import com.tencent.supersonic.chat.core.plugin.PluginParseConfig;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FunctionReq {

    private String queryText;

    private List<PluginParseConfig> pluginConfigs;

}
