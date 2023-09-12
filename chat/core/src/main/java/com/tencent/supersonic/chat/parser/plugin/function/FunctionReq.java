package com.tencent.supersonic.chat.parser.plugin.function;

import java.util.List;

import com.tencent.supersonic.chat.plugin.PluginParseConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FunctionReq {

    private String queryText;

    private List<PluginParseConfig> pluginConfigs;

}
