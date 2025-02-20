package com.tencent.supersonic.common.pojo;

import com.tencent.supersonic.common.util.AESEncryptionUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatModelConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private String apiVersion;
    private Double temperature = 0.0d;
    private Long timeOut = 60L;
    private String endpoint;
    private String secretKey;
    private Double topP;
    private Integer maxRetries = 3;
    private Boolean logRequests = false;
    private Boolean logResponses = false;
    private Boolean enableSearch = false;

    public String keyDecrypt() {
        return AESEncryptionUtil.aesDecryptECB(getApiKey());
    }
}
