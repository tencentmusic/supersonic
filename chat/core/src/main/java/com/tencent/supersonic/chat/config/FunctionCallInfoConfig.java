package com.tencent.supersonic.chat.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class FunctionCallInfoConfig {
    @Value("${functionCall.url:}")
    private String url;

    @Value("${funtionCall.plugin.select.path:/plugin_selection}")
    private String pluginSelectPath;
}
