package com.tencent.supersonic.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "s2.encryption")
public class EncryptionProperties {
    private String aesKey;
    private String aesIv;
}
