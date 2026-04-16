package com.tencent.supersonic.headless.server.service.delivery;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Centralised configuration for report download signed URLs. All callers in this module must inject
 * this bean instead of duplicating the @Value fallback chain. Note: CardActionHandler
 * (feishu/server) retains its own @Value because feishu/server does not depend on headless/server.
 * Keep the fallback chain there in sync with this class.
 */
@Component
@Getter
public class ReportDownloadProperties {

    // NOTE: Keep this fallback chain in sync with CardActionHandler in feishu/server.
    @Value("${s2.report-download.signing-secret:${s2.report.download.signing-secret:${s2.encryption.aes-key:}}}")
    private String signingSecret;

    @Value("${s2.report-download.token-ttl-seconds:604800}")
    private long tokenTtlSeconds;
}
