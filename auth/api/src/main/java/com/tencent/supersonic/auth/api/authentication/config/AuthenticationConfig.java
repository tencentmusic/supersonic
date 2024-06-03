package com.tencent.supersonic.auth.api.authentication.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class AuthenticationConfig {

    @Value("${authentication.exclude.path:XXX}")
    private String excludePath;

    @Value("${authentication.include.path:/api}")
    private String includePath;

    @Value("${authentication.enable:false}")
    private boolean enabled;

    @Value("${authentication.token.secret:secret}")
    private String tokenSecret;

    @Value("${authentication.token.http.header.key:Authorization}")
    private String tokenHttpHeaderKey;

    @Value("${authentication.app.appId:appId}")
    private String appId;

    @Value("${authentication.app.timestamp:timestamp}")
    private String timestamp;

    @Value("${authentication.app.signature:signature}")
    private String signature;

    @Value("${authentication.token.timeout:7200000}")
    private Long tokenTimeout;
}
