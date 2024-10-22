package com.tencent.supersonic.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DifyClient {
    private static final String DEFAULT_USER = "zhaodongsheng";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private String difyURL;
    private String difyKey;

    public DifyClient(String difyURL, String difyKey) {
        this.difyURL = difyURL;
        this.difyKey = difyKey;
    }

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

    public DifyResult generate(Map<String, String> inputs, String queryText, String user,
            String conversationId) {
        Map<String, String> headers = defaultHeaders();
        DifyRequest request = new DifyRequest();
        request.setInputs(inputs);
        request.setQuery(queryText);
        request.setUser(user);
        if (conversationId != null && !conversationId.isEmpty()) {
            request.setConversationId(conversationId);
        }
        return sendRequest(request, headers);
    }

    public DifyResult sendRequest(DifyRequest request, Map<String, String> headers) {
        try {
            log.debug("请求dify- header--->" + JsonUtil.toString(headers));
            log.debug("请求dify- conversionId--->" + JsonUtil.toString(request));
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
        if (difyKey.contains("Bearer")) {
            headers.put("Authorization", difyKey);
        } else {
            headers.put("Authorization", "Bearer " + difyKey);
        }

        headers.put("Content-Type", CONTENT_TYPE_JSON);
        return headers;
    }

}
