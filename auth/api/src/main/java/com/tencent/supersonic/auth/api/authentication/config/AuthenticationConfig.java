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

}
