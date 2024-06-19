package com.tencent.supersonic.headless.chat.parser.llm;

import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.chat.utils.HttpUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    public String parseSQLResult(String sql) {
        Pattern pattern = Pattern.compile("```(sql)?(.*)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) {
            return sql.trim();
        } else {
            return matcher.group(2).trim();
        }
    }

    private Map<String, String> defaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", difyKey);
        headers.put("Content-Type", CONTENT_TYPE_JSON);
        return headers;
    }

}
