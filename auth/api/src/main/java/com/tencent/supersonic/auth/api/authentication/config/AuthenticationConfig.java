package com.tencent.supersonic.auth.api.authentication.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Configuration
public class AuthenticationConfig {

    @Value("${s2.authentication.exclude.path:XXX}")
    private String excludePath;

    @Value("${s2.authentication.include.path:/api}")
    private String includePath;

    @Value("${s2.authentication.strategy:http}")
    private String strategy;

    @Value("${s2.authentication.enable:false}")
    private boolean enabled;

    @Value("${s2.authentication.token.default.appKey:supersonic}")
    private String tokenDefaultAppKey;

    @Value("${s2.authentication.token.appSecret:supersonic:WIaO9YRRVt+7QtpPvyWsARFngnEcbaKBk"
            + "783uGFwMrbJBaochsqCH62L4Kijcb0sZCYoSsiKGV/zPml5MnZ3uQ==}")
    private String tokenAppSecret;

    @Value("${s2.authentication.token.http.header.key:Authorization}")
    private String tokenHttpHeaderKey;

    @Value("${s2.authentication.token.http.app.key:App-Key}")
    private String tokenHttpHeaderAppKey;

    @Value("${s2.authentication.app.appId:appId}")
    private String appId;

    @Value("${s2.authentication.app.timestamp:timestamp}")
    private String timestamp;

    @Value("${s2.authentication.app.signature:signature}")
    private String signature;

    @Value("${s2.authentication.token.timeout:72000000}")
    private Long tokenTimeout;

    public Map<String, String> getAppKeyToSecretMap() {
        return Arrays.stream(this.tokenAppSecret.split(",")).map(s -> s.split(":"))
                .collect(Collectors.toMap(e -> e[0].trim(), e -> e[1].trim()));
    }
}
