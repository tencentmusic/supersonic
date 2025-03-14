package com.tencent.supersonic.common.util;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;

@Slf4j
public class DifyClient {
    private static final String DEFAULT_USER = "zhaodongsheng";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private String difyURL;
    private String difyKey;
    
    private final OkHttpClient okHttpClient;

    public DifyClient(String difyURL, String difyKey) {
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        this.okHttpClient = okHttpClientBuilder.build();
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
    
    public void streamingGenerate(String prompt, String user, StreamingResponseHandler<AiMessage> handler) {
        Map<String, String> headers = defaultHeaders();
        DifyRequest request = new DifyRequest();
        request.setQuery(prompt);
        request.setUser(user);
        request.setResponseMode("streaming");
        EventSourceListener eventSourceListener = new EventSourceListener() {
            
            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                JSONObject object = JSON.parseObject(data);
                String event = object.getString("event");
                if ("message".equals(event)) {
                    handler.onNext(object.getString("answer"));
                }
            }
            
        };
        Request.Builder builder = new Request.Builder();
        builder.url(difyURL).headers(Headers.of(headers)).post(RequestBody.create(JSON.toJSONBytes(request)));
        EventSources.createFactory(this.okHttpClient)
            .newEventSource(
                    builder.build(),
                    eventSourceListener
            );
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
