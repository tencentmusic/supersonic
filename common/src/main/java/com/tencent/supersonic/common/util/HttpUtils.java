package com.tencent.supersonic.common.util;

import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    // 重试参考：okhttp3.RealCall.getResponseWithInterceptorChain
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(3, TimeUnit.MINUTES).retryOnConnectionFailure(true).build();

    static {
        Dispatcher dispatcher = client.dispatcher();
        dispatcher.setMaxRequestsPerHost(300);
        dispatcher.setMaxRequests(200);
    }

    public static Response execute(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        return client.newCall(request).execute();
    }

    public static String get(String url) throws IOException {
        return doRequest(builder(url).build());
    }

    public static String get(String url, Map<String, String> headers) throws IOException {
        return doRequest(headerBuilder(url, headers).build());
    }

    public static String get(String url, Map<String, String> headers, Map<String, Object> params)
            throws IOException {
        return doRequest(headerBuilder(url + buildUrlParams(params), headers).build());
    }

    public static <T> T get(String url, Class<T> classOfT) throws IOException {
        return doRequest(builder(url).build(), classOfT);
    }

    public static <T> T get(String url, Map<String, String> headers, Class<T> classOfT)
            throws IOException {
        return doRequest(headerBuilder(url, headers).build(), classOfT);
    }

    public static <T> T get(String url, Map<String, String> headers, Map<String, Object> params,
            Class<T> classOfT) throws IOException {
        return doRequest(headerBuilder(url + buildUrlParams(params), headers).build(), classOfT);
    }

    // public static <T> T get(String url, TypeReference<T> type) throws IOException {
    // return doRequest(builder(url).build(), type);
    // }

    // public static <T> T get(String url, Map<String, String> headers, TypeReference<T> type)
    // throws IOException {
    // return doRequest(headerBuilder(url, headers).build(), type);
    // }

    // public static <T> T get(String url, Map<String, String> headers, Map<String, Object> params,
    // TypeReference<T> type) throws IOException {
    // return doRequest(headerBuilder(url + buildUrlParams(params), headers).build(), type);
    // }

    public static String post(String url, Object body) throws IOException {
        return doRequest(postRequest(url, body));
    }

    public static String post(String url, Object body, Map<String, String> headers)
            throws IOException {
        return doRequest(postRequest(url, body, headers));
    }

    public static <T> T post(String url, Object body, Class<T> classOfT) throws IOException {
        return doRequest(postRequest(url, body), classOfT);
    }

    // public static <T> T post(String url, Object body, TypeReference<T> type) throws IOException {
    // return doRequest(postRequest(url, body), type);
    // }

    public static <T> T post(String url, Object body, Map<String, String> headers,
            Class<T> classOfT) throws IOException {
        return doRequest(postRequest(url, body, headers), classOfT);
    }

    // public static <T> T post(String url, Object body, Map<String, String> headers,
    // TypeReference<T> type) throws IOException {
    // return doRequest(postRequest(url, body, headers), type);
    // }

    private static Request postRequest(String url, Object body) {
        return builder(url).post(buildRequestBody(body, null)).build();
    }

    private static Request postRequest(String url, Object body, Map<String, String> headers) {
        return headerBuilder(url, headers).post(buildRequestBody(body, headers)).build();
    }

    private static Request.Builder builder(String url) {
        return new Request.Builder().url(url);
    }

    private static Request.Builder headerBuilder(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().url(url);
        headers.forEach(builder::addHeader);

        return builder;
    }

    private static <T> T doRequest(Request request, Class<T> classOfT) throws IOException {
        return JsonUtil.toObject(doRequest(request), classOfT);
    }

    // private static <T> T doRequest(Request request, TypeReference<T> type) throws IOException {
    // return JsonUtil.toObject(doRequest(request), type);
    // }

    private static String doRequest(Request request) throws IOException {
        long beginTime = System.currentTimeMillis();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                throw new RuntimeException(
                        "Http请求失败[" + response.code() + "]:" + response.body().string() + "...");
            }
        } finally {
            logger.info("begin to request : {}, execute costs(ms) : {}", request.url(),
                    System.currentTimeMillis() - beginTime);
        }
    }

    private static RequestBody buildRequestBody(Object body, Map<String, String> headers) {
        if (headers != null && headers.containsKey("Content-Type")) {
            String contentType = headers.get("Content-Type");
            return RequestBody.create(MediaType.parse(contentType), body.toString());
        }

        if (body instanceof String && ((String) body).contains("=")) {
            return RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                    (String) body);
        }

        return RequestBody.create(MediaType.parse("application/json"), JsonUtil.toString(body));
    }

    private static String buildUrlParams(Map<String, Object> params) {
        if (params.isEmpty()) {
            return "";
        }

        return "?" + params.entrySet().stream().map(it -> it.getKey() + "=" + it.getValue())
                .collect(Collectors.joining("&"));
    }
}
