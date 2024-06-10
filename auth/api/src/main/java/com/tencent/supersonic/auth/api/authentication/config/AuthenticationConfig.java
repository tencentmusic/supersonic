package com.tencent.supersonic.auth.api.authentication.config;


import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
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

    @Value("${authentication.token.default.appKey:supersonic}")
    private String tokenDefaultAppKey;

    @Value("${authentication.token.appSecret:supersonic:secret}")
    private String tokenAppSecret;

    @Value("${authentication.token.http.header.key:Authorization}")
    private String tokenHttpHeaderKey;

    @Value("${authentication.token.http.app.key:App-Key}")
    private String tokenHttpHeaderAppKey;

    @Value("${authentication.app.appId:appId}")
    private String appId;

    @Value("${authentication.app.timestamp:timestamp}")
    private String timestamp;

    @Value("${authentication.app.signature:signature}")
    private String signature;

    @Value("${authentication.token.timeout:7200000}")
    private Long tokenTimeout;

    public Map<String, String> getAppKeyToSecretMap() {
        return Arrays.stream(this.tokenAppSecret.split(","))
                .map(s -> s.split(":"))
                .collect(Collectors.toMap(e -> e[0].trim(), e -> e[1].trim()));
    }
}
