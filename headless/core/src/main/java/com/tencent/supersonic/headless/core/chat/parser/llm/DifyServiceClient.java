package com.tencent.supersonic.headless.core.chat.parser.llm;

import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.core.utils.HttpUtils;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DifyServiceClient {
    private static final String DEFAULT_USER = "default";
    private static final String CONTENT_TYPE_JSON = "application/json";

    @Value("${dify.url}")
    private String difyURL;

    @Value("${dify.key}")
    private String difyKey;

    public DifyResult generate(String prompt) {
        Map<String, String> headers = defaultHeaders();
        DifyRequest request = new DifyRequest();
        request.setQuery(prompt);
        request.setUser(DEFAULT_USER);
        return sendRequest(request, headers);
    }

    public DifyResult generate(String prompt, String user) {
        Map<String, String> headers = defaultHeaders();
        DifyRequest request = new DifyRequest();
        request.setQuery(prompt);
        request.setUser(user);
        return sendRequest(request, headers);
    }

    public DifyResult sendRequest(DifyRequest request, Map<String, String> headers) {
        try {
            return HttpUtils.post(difyURL, JsonUtil.toString(request), headers, DifyResult.class);
        } catch (Exception e) {
            log.error("请求dify失败---->" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> defaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", difyKey);
        headers.put("Content-Type", CONTENT_TYPE_JSON);
        return headers;
    }

}
