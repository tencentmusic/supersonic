package com.tencent.supersonic.common.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.CharsetUtils;
import org.apache.http.util.EntityUtils;
import org.springframework.util.CollectionUtils;

@Slf4j
public class HttpClientUtils {
    /***
     * Encoding format. Unified sending encoding format using UTF-8
     */
    private static final String ENCODING = "UTF-8";

    /**
     * Default number of connections per route
     */
    private static final int DEFAULT_MAX_CONN_PERHOST = 100;

    /**
     * Maximum number of connections in the entire Connection pool
     */
    private static final int DEFAULT_MAX_TOTAL_CONN = 200;

    private static final int DEFAULT_CONNECTION_TIMEOUT = 150000;
    private static final int DEFAULT_READ_TIMEOUT = 150000;
    private static final int DEFAULT_CONN_REQUEST_TIMEOUT = 100000;

    // httpClient singleton
    private static CloseableHttpClient httpClient = null;

    static {
        init();
    }

    private static void init() {
        try {
            SSLConnectionSocketFactory sslConnectionSocketFactory =
                    new SSLConnectionSocketFactory(
                            SSLContexts.custom().loadTrustMaterial((chain, authType) -> true).build(),
                            new String[]{"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"},
                            null,
                            NoopHostnameVerifier.INSTANCE);

            PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
                    RegistryBuilder.<ConnectionSocketFactory>create()
                            .register("http", PlainConnectionSocketFactory.getSocketFactory())
                            .register("https", sslConnectionSocketFactory).build()
            );
            connManager.setMaxTotal(DEFAULT_MAX_TOTAL_CONN);
            connManager.setDefaultMaxPerRoute(DEFAULT_MAX_CONN_PERHOST);

            RequestConfig requestConfig = RequestConfig.custom()
                    // 请求超时时间
                    .setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT)
                    // 等待数据超时时间
                    .setSocketTimeout(DEFAULT_READ_TIMEOUT)
                    // 连接不够用时等待超时时间
                    .setConnectionRequestTimeout(DEFAULT_CONN_REQUEST_TIMEOUT)
                    .build();

            HttpRequestRetryHandler httpRequestRetryHandler = (exception, executionCount, context) -> {
                // 如果已经重试了3次，就放弃
                if (executionCount > 3) {
                    log.warn("Maximum tries reached, exception would be thrown to outer block");
                    return false;
                }
                if (exception instanceof NoHttpResponseException) {
                    // 如果服务器丢掉了连接，那么就重试
                    log.warn("Retry, No response from server on  {}  error: {}", executionCount,
                            exception.getMessage());
                    return true;
                } else if (exception instanceof SocketException) {
                    // 如果服务器断开了连接，那么就重试
                    log.warn("Retry, No connection from server on {} error: {}", executionCount,
                            exception.getMessage());
                    return true;
                }
                return false;
            };

            httpClient = HttpClients.custom()
                    // 设置连接池
                    .setConnectionManager(connManager)
                    // 设置超时时间
                    .setDefaultRequestConfig(requestConfig)
                    // 设置连接存活时间
                    .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
                        @Override
                        public long getKeepAliveDuration(final HttpResponse response, final HttpContext context) {
                            long keepAlive = super.getKeepAliveDuration(response, context);
                            if (keepAlive == -1) {
                                keepAlive = 5000;
                            }
                            return keepAlive;
                        }
                    })
                    .setRetryHandler(httpRequestRetryHandler)
                    // 设置连接存活时间
                    .setConnectionTimeToLive(5000L, TimeUnit.MILLISECONDS)
                    // 关闭无效和空闲的连接
                    .evictIdleConnections(5L, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Send a post request; Without request header and request parameters
     *
     * @param url
     * @return
     * @throws Exception
     */
    public static HttpClientResult doPost(String url) {
        return doPost(url, null, null, null, null);
    }

    /**
     * Send a post request; With request parameters
     *
     * @param url
     * @param params
     * @return
     * @throws Exception
     */
    public static HttpClientResult doPost(String url, Map<String, String> params) {
        return doPost(url, null, null, null, params);
    }

    /**
     * post
     *
     * @return
     */
    public static HttpClientResult doPost(String url,
            String proxyHost,
            Integer proxyPort,
            Map<String, String> headers,
            Map<String, String> params) {
        return RetryUtils.exec(() -> {
            HttpPost httpPost = null;
            CloseableHttpResponse response = null;
            try {
                httpPost = new HttpPost(url);
                setProxy(httpPost, proxyHost, proxyPort);

                // 封装header参数
                packageHeader(headers, httpPost);
                // 封装请求参数
                packageParam(params, httpPost);

                response = httpClient.execute(httpPost);
                // 获取返回结果
                HttpClientResult result = getHttpClientResult(response);
                log.info("uri:{}, req:{}, resp:{}", url,
                        "headers:" + getHeaders(httpPost) + "------params:" + params, result);
                return result;
            } catch (Exception e) {
                log.error("uri:{}, req:{}", url, "headers:" + headers + "------params:" + params, e);
                throw new RuntimeException(e.getMessage());
            } finally {
                close(httpPost, response);
            }
        });
    }

    /**
     * for us scheduling
     *
     * @param url
     * @param header
     * @param params
     * @return
     */
    public static HttpClientResult doPost(String url, Map<String, String> header, Map<String, String> params) {
        return doPost(url, null, null, header, params);
    }

    /**
     * Send a get request; Without request header and request parameters
     *
     * @param url
     * @return
     * @throws Exception
     */
    public static HttpClientResult doGet(String url) {
        return doGet(url, null, null, null, null);
    }

    /**
     * Send a get request; With request parameters
     *
     * @param url
     * @param params
     * @return
     * @throws Exception
     */
    public static HttpClientResult doGet(String url, Map<String, String> params) {
        return doGet(url, null, null, null, params);
    }

    /**
     * Send a get request; With request header and request parameters
     *
     * @param url
     * @param headers
     * @param params
     * @return
     * @throws Exception
     */
    public static HttpClientResult doGet(String url, String proxyHost, Integer proxyPort, Map<String, String> headers,
            Map<String, String> params) {
        return RetryUtils.exec(() -> {
            HttpGet httpGet = null;
            CloseableHttpResponse response = null;
            try {
                // 创建访问的地址
                URIBuilder uriBuilder = new URIBuilder(url);
                if (params != null) {
                    Set<Map.Entry<String, String>> entrySet = params.entrySet();
                    for (Map.Entry<String, String> entry : entrySet) {
                        uriBuilder.setParameter(entry.getKey(), entry.getValue());
                    }
                }

                httpGet = new HttpGet(uriBuilder.build());
                setProxy(httpGet, proxyHost, proxyPort);

                // 设置请求头
                packageHeader(headers, httpGet);

                response = httpClient.execute(httpGet);

                // 获取返回结果
                HttpClientResult res = getHttpClientResult(response);
                log.debug("GET uri:{}, req:{}, resp:{}", url,
                        "headers:" + getHeaders(httpGet) + "------params:" + params, res);
                return res;
            } catch (Exception e) {
                log.error("GET error! uri:{}, req:{}", url, "headers:" + headers + "------params:" + params, e);
                throw new RuntimeException(e.getMessage());
            } finally {
                close(httpGet, response);
            }
        });
    }

    /**
     * for us scheduling
     *
     * @param url
     * @param header
     * @param params
     * @return
     */
    public static HttpClientResult doGet(String url, Map<String, String> header, Map<String, String> params) {
        return doGet(url, null, null, header, params);
    }

    /**
     *
     */
    public static void close(HttpRequestBase httpRequest, CloseableHttpResponse response) {
        if (response != null) {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (Throwable ex) {
                log.error("entity close error : ", ex);
            }
            try {
                response.close();
            } catch (Throwable ex) {
                log.error("response close error : ", ex);
            }

        }
        if (httpRequest != null) {
            try {
                httpRequest.abort();
            } catch (Throwable ex) {
                log.error("httpPost abort error : ", ex);
            }
        }
    }

    /**
     * @param headers
     * @param httpMethod
     */
    public static void packageHeader(Map<String, String> headers, HttpRequestBase httpMethod) {
        if (headers != null) {
            Set<Map.Entry<String, String>> entrySet = headers.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                httpMethod.setHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    public static String getHeaders(HttpRequestBase request) {
        if (request == null) {
            return "";
        }

        StringBuilder headsString = new StringBuilder("");
        Header[] heads = request.getAllHeaders();
        if (heads != null) {
            for (int i = 0; i < heads.length; i++) {
                headsString.append(heads[i]).append(" , ");
            }
        }
        return headsString.toString();
    }

    /**
     * Description: pack params
     *
     * @param params
     * @param httpMethod
     * @throws UnsupportedEncodingException
     */
    public static void packageParam(Map<String, String> params, HttpEntityEnclosingRequestBase httpMethod)
            throws UnsupportedEncodingException {
        if (params != null) {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            Set<Map.Entry<String, String>> entrySet = params.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }

            // 设置到请求的http对象中
            httpMethod.setEntity(new UrlEncodedFormEntity(nvps, ENCODING));
        }
    }

    public static void setProxy(HttpRequestBase httpMethod, String proxyHost, Integer proxyPort) {
        if (!StringUtils.isEmpty(proxyHost) && proxyPort != null) {
            RequestConfig config = RequestConfig.custom()
                    .setProxy(new HttpHost(proxyHost, proxyPort))
                    .setConnectTimeout(10000)
                    .setSocketTimeout(10000)
                    .setConnectionRequestTimeout(3000)
                    .build();
            httpMethod.setConfig(config);
        }
    }

    /**
     * is respond successfully --200
     */
    public static boolean isOK(HttpClientResult res) {
        return res != null && res.getCode() == HttpStatus.SC_OK;
    }

    /**
     * post json
     *
     * @return
     */
    public static HttpClientResult doPostJSON(String url, String proxyHost, Integer proxyPort,
            Map<String, String> headers, String req) {
        return RetryUtils.exec(() -> {
            HttpPost httpPost = null;
            CloseableHttpResponse response = null;
            try {
                httpPost = new HttpPost(url);
                setProxy(httpPost, proxyHost, proxyPort);

                // 封装header参数
                packageHeader(headers, httpPost);
                httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");

                // 封装请求参数
                StringEntity stringEntity = new StringEntity(req, ENCODING); // 解决中文乱码问题
                stringEntity.setContentEncoding("UTF-8");

                httpPost.setEntity(stringEntity);

                response = httpClient.execute(httpPost);
                // 获取返回结果
                HttpClientResult res = getHttpClientResult(response);
                log.info("doPostJSON uri:{}, req:{}, resp:{}", url,
                        "headers:" + getHeaders(httpPost) + "------req:" + req, res);
                return res;
            } catch (Exception e) {
                log.error("doPostJSON error! uri:{}, req:{}", url, "headers:" + headers + "------req:" + req, e);
                throw new RuntimeException(e.getMessage());
            } finally {
                close(httpPost, response);
            }
        });
    }

    public static HttpClientResult doPostJSON(String url, String req) {
        return doPostJSON(url, null, null, null, req);
    }

    /**
     * get json
     */
    public static HttpClientResult doGetJSON(String url, String proxyHost, Integer proxyPort,
            Map<String, String> headers, Map<String, String> params) {
        return RetryUtils.exec(() -> {
            HttpGet httpGet = null;
            CloseableHttpResponse response = null;
            try {
                // 创建访问的地址
                URIBuilder uriBuilder = new URIBuilder(url);
                if (params != null) {
                    Set<Map.Entry<String, String>> entrySet = params.entrySet();
                    for (Map.Entry<String, String> entry : entrySet) {
                        uriBuilder.setParameter(entry.getKey(), entry.getValue());
                    }
                }

                httpGet = new HttpGet(uriBuilder.build());
                setProxy(httpGet, proxyHost, proxyPort);

                // 设置请求头
                packageHeader(headers, httpGet);
                httpGet.setHeader("Content-Type", "application/json;charset=UTF-8");

                response = httpClient.execute(httpGet);

                // 获取返回结果
                HttpClientResult res = getHttpClientResult(response);

                log.info("doGetJSON uri:{}, req:{}, resp:{}", url,
                        "headers:" + getHeaders(httpGet) + "------params:" + params, res);

                return res;
            } catch (Exception e) {
                log.warn("doGetJSON error! uri:{}, req:{}", url, "headers:" + headers + "------params:" + params, e);
                throw new RuntimeException(e.getMessage());
            } finally {
                close(httpGet, response);
            }
        });
    }

    private static HttpClientResult getHttpClientResult(CloseableHttpResponse response) throws IOException {
        HttpEntity entity;
        HttpClientResult res = new HttpClientResult(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        if (response != null && response.getStatusLine() != null) {
            String content = "";
            entity = response.getEntity();
            if (entity != null) {
                content = EntityUtils.toString(entity, ENCODING);
            }
            res = new HttpClientResult(response.getStatusLine().getStatusCode(), content);
        }
        return res;
    }

    /**
     * for us scheduling
     *
     * @param url
     * @param headers
     * @param bodyParams
     * @param fullFilePath
     * @return
     */
    public static HttpClientResult doFileUploadBodyParams(String url, Map<String, String> headers,
            Map<String, String> bodyParams,
            String fullFilePath) {
        return doFileUpload(url, null, null, headers, null, bodyParams, fullFilePath);
    }

    public static HttpClientResult doFileUpload(String url,
            String proxyHost,
            Integer proxyPort,
            Map<String, String> headers,
            Map<String, String> params,
            Map<String, String> bodyParams,
            String fullFilePath) {
        return RetryUtils.exec(() -> {
            InputStream inputStream = null;
            CloseableHttpResponse response = null;
            HttpPost httpPost = null;
            try {

                File uploadFile = new File(fullFilePath);
                inputStream = new FileInputStream(uploadFile);

                httpPost = new HttpPost(url);
                setProxy(httpPost, proxyHost, proxyPort);

                packageHeader(headers, httpPost);

                HttpEntity entity = getFileUploadHttpEntity(params, bodyParams, inputStream, uploadFile.getName());
                httpPost.setEntity(entity);

                response = httpClient.execute(httpPost);
                // 执行请求并获得响应结果
                HttpClientResult res = getHttpClientResult(response);
                log.info("doFileUpload uri:{}, req:{}, resp:{}", url,
                        "params:" + params + ", fullFilePath:" + fullFilePath, res);
                return res;
            } catch (Exception e) {
                log.error("doFileUpload error! uri:{}, req:{}", url,
                        "params:" + params + ", fullFilePath:" + fullFilePath, e);
                throw new RuntimeException(e.getMessage());
            } finally {
                try {
                    if (null != inputStream) {
                        inputStream.close();
                    }
                    // 释放资源
                    close(httpPost, response);
                } catch (IOException e) {
                    log.error("HttpClientUtils release error!", e);
                }
            }
        });
    }

    private static HttpEntity getFileUploadHttpEntity(Map<String, String> params,
            Map<String, String> bodyParams,
            InputStream inputStream,
            String fileName) throws UnsupportedEncodingException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addBinaryBody("file", inputStream, ContentType.create("multipart/form-data"), fileName);

        if (!CollectionUtils.isEmpty(bodyParams)) {
            for (String bodyParamsKey : bodyParams.keySet()) {
                builder.addTextBody(bodyParamsKey, bodyParams.get(bodyParamsKey));
            }
        }
        //构建请求参数 普通表单项
        if (!CollectionUtils.isEmpty(params)) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.addPart(entry.getKey(), new StringBody(entry.getValue(), ContentType.MULTIPART_FORM_DATA));
            }
        }

        builder.setCharset(CharsetUtils.get(ENCODING));
        return builder.build();
    }

    /**
     * for us scheduling， send delete
     *
     * @param url
     * @param headers
     * @param req
     * @return
     */
    public static HttpClientResult doDelete(String url, Map<String, String> headers, String req) {
        return RetryUtils.exec(() -> {
            HttpDeleteWithBody httpDelete = null;
            CloseableHttpResponse response = null;
            try {
                httpDelete = new HttpDeleteWithBody(url);
                // 封装header参数
                packageHeader(headers, httpDelete);
                httpDelete.setHeader("Content-Type", "application/json;charset=UTF-8");
                // 封装请求参数
                StringEntity stringEntity = new StringEntity(req, ENCODING); // 解决中文乱码问题
                stringEntity.setContentEncoding("UTF-8");

                httpDelete.setEntity(stringEntity);

                response = httpClient.execute(httpDelete);

                HttpClientResult res = getHttpClientResult(response);
                log.info("doDeleteJSON uri:{}, req:{}, resp:{}", url,
                        "headers:" + getHeaders(httpDelete) + "------req:" + req, res);
                return res;
            } catch (Exception e) {
                log.error("doDeleteJSON error! uri:{}, req:{}", url, "headers:" + headers + "------req:" + req, e);
                throw new RuntimeException(e.getMessage());
            } finally {
                close(httpDelete, response);
            }
        });
    }


    private static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {

        public static final String METHOD_NAME = "DELETE";

        public HttpDeleteWithBody() {
            super();
        }

        public HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        public HttpDeleteWithBody(final URI uri) {
            super();
            setURI(uri);
        }

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }
    }

    public static HttpClientResult doPutJson(String url, Map<String, String> headers, String req) {
        return RetryUtils.exec(() -> {
            HttpPut httpPut = null;
            CloseableHttpResponse response = null;
            try {
                httpPut = new HttpPut(url);
                // 封装header参数
                packageHeader(headers, httpPut);
                httpPut.setHeader("Content-Type", "application/json;charset=UTF-8");
                // 封装请求参数
                StringEntity stringEntity = new StringEntity(req, ENCODING); // 解决中文乱码问题
                stringEntity.setContentEncoding("UTF-8");
                httpPut.setEntity(stringEntity);
                response = httpClient.execute(httpPut);
                HttpClientResult res = getHttpClientResult(response);
                log.info("doPutJSON uri:{}, req:{}, resp:{}", url,
                        "headers:" + getHeaders(httpPut) + "------req:" + req, res);
                return res;
            } catch (Exception e) {
                log.error("doPutJSON error! uri:{}, req:{}", url, "headers:" + headers + "------req:" + req, e);
                throw new RuntimeException(e.getMessage());
            } finally {
                close(httpPut, response);
            }
        });
    }

}
