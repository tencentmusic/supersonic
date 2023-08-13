package com.tencent.supersonic.auth.authentication.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class TppConfig {

    @Value(value = "${auth.app.secret:}")
    private String appSecret;

    @Value(value = "${auth.app.key:}")
    private String appKey;

    @Value(value = "${auth.oa.url:}")
    private String tppOaUrl;

}
