package com.tencent.supersonic.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "s2.storage")
public class StorageProperties {

    private String type = "local";
    private String prefix = "exports";

    private Local local = new Local();
    private Oss oss = new Oss();
    private S3 s3 = new S3();

    @Data
    public static class Local {
        private String rootDir = System.getProperty("java.io.tmpdir") + "/supersonic-export";
    }

    @Data
    public static class Oss {
        private String endpoint;
        private String bucket;
        private String accessKeyId;
        private String accessKeySecret;
    }

    @Data
    public static class S3 {
        private String endpoint;
        private String region;
        private String bucket;
        private String accessKey;
        private String secretKey;
        private boolean pathStyle = false;
        /** When true, bypass HTTP_PROXY/HTTPS_PROXY for requests to the S3 endpoint. */
        private boolean noProxy = false;
    }
}
