package com.tencent.supersonic.chat.parser.plugin.function;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class FunctionCallConfig {
    @Value("${functionCall.url:}")
    private String url;

    @Value("${funtionCall.plugin.select.path:/plugin_selection}")
    private String pluginSelectPath;
}
