package com.tencent.supersonic.auth.api.authentication.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
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

    @Value("${s2.authentication.token.appSecret}")
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

    @PostConstruct
    public void validateTokenAppSecret() {
        Map<String, String> secrets;
        try {
            secrets = getAppKeyToSecretMap();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "s2.authentication.token.appSecret must contain comma-separated appKey:secret mappings");
        }
        if (secrets.isEmpty()) {
            throw new IllegalArgumentException("s2.authentication.token.appSecret must not be empty");
        }
        for (Map.Entry<String, String> entry : secrets.entrySet()) {
            String secret = entry.getValue();
            if (entry.getKey().isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 64
                    || secret.equals("WIaO9YRRVt+7QtpPvyWsARFngnEcbaKBk"
                            + "783uGFwMrbJBaochsqCH62L4Kijcb0sZCYoSsiKGV/zPml5MnZ3uQ==")) {
                throw new IllegalArgumentException(
                        "JWT signing secrets must be unique random values of at least 64 bytes, "
                                + "not the published default; generate one with openssl rand -hex 64");
            }
        }
    }

    public Map<String, String> getAppKeyToSecretMap() {
        return Arrays.stream(this.tokenAppSecret.split(",")).map(s -> s.split(":"))
                .collect(Collectors.toMap(e -> e[0].trim(), e -> e[1].trim()));
    }
}
