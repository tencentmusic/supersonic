package com.tencent.supersonic.config;



import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.LaxRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class RestTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);

    static {
        // JDK 不读 UNIX 风格的 HTTPS_PROXY / HTTP_PROXY / NO_PROXY 环境变量,只认 system property。
        // 这里在类加载时把 shell 里的环境变量翻译成 JDK system property,让后续 useSystemProperties()
        // 能够正确生效。NO_PROXY 用逗号分隔(curl/wget 约定),http.nonProxyHosts 用竖线分隔,需要转换。
        bridgeProxyEnvToSystemProperties();
    }

    @Bean
    public RestTemplate restTemplate() {

        // SO_TIMEOUT must be set on the connection manager, not just RequestConfig.
        // Without it, TLS handshakes can hang forever when a proxy stalls — Spring's
        // setReadTimeout only configures responseTimeout, which doesn't apply during
        // socket read in the handshake phase.
        SocketConfig socketConfig =
                SocketConfig.custom().setSoTimeout(Timeout.ofSeconds(15)).build();
        ConnectionConfig connectionConfig =
                ConnectionConfig.custom().setConnectTimeout(Timeout.ofSeconds(10))
                        .setSocketTimeout(Timeout.ofSeconds(15)).build();
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultSocketConfig(socketConfig)
                        .setDefaultConnectionConfig(connectionConfig).build();

        // useSystemProperties() 让 HttpClient 5 读取 https.proxyHost / https.proxyPort /
        // http.nonProxyHosts 等 JDK system property,从而既走代理又能让 NO_PROXY 中列出的
        // 飞书域名直连。前面的 static 块已把 shell 环境变量同步到 system property。
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setRedirectStrategy(new LaxRedirectStrategy()).useSystemProperties().build();

        HttpComponentsClientHttpRequestFactory httpRequestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        httpRequestFactory.setConnectionRequestTimeout(2000);
        httpRequestFactory.setConnectTimeout(10000);
        httpRequestFactory.setReadTimeout(10000);

        RestTemplate restTemplate = new RestTemplate(httpRequestFactory);
        restTemplate.getMessageConverters().set(1,
                new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }

    private static void bridgeProxyEnvToSystemProperties() {
        applyProxy("https",
                firstNonBlank(System.getenv("HTTPS_PROXY"), System.getenv("https_proxy")));
        applyProxy("http", firstNonBlank(System.getenv("HTTP_PROXY"), System.getenv("http_proxy")));

        String noProxy = firstNonBlank(System.getenv("NO_PROXY"), System.getenv("no_proxy"));
        if (noProxy != null && System.getProperty("http.nonProxyHosts") == null) {
            // curl/wget 用逗号分隔; JDK 用竖线分隔,且通配符是 *.example.com 形式。
            // CIDR (172.16.0.0/12) 这类 JDK 不认,直接丢掉避免污染匹配规则。
            String converted =
                    Arrays.stream(noProxy.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                            .filter(s -> !s.contains("/")).collect(Collectors.joining("|"));
            if (!converted.isEmpty()) {
                System.setProperty("http.nonProxyHosts", converted);
                log.info("RestTemplate proxy bridge: http.nonProxyHosts={}", converted);
            }
        }
    }

    private static void applyProxy(String scheme, String url) {
        if (url == null) {
            return;
        }
        try {
            URI uri = URI.create(url);
            if (uri.getHost() == null || uri.getPort() <= 0) {
                return;
            }
            if (System.getProperty(scheme + ".proxyHost") == null) {
                System.setProperty(scheme + ".proxyHost", uri.getHost());
                System.setProperty(scheme + ".proxyPort", String.valueOf(uri.getPort()));
                log.info("RestTemplate proxy bridge: {}.proxyHost={}:{}", scheme, uri.getHost(),
                        uri.getPort());
            }
        } catch (Exception e) {
            log.warn("Failed to parse {} proxy URL from env: {}", scheme, url, e);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
