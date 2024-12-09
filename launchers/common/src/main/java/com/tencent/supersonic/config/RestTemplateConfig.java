package com.tencent.supersonic.config;



import org.apache.hc.client5.http.impl.LaxRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {

        // HttpClient 5 较新版本才会有LaxRedirectStrategy, 所以我重新定义了
        CloseableHttpClient httpClient =
                // HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy()) //
                // 使用宽松重定向策略
                HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()) // 使用宽松重定向策略
                        .build();

        HttpComponentsClientHttpRequestFactory httpRequestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        httpRequestFactory.setConnectionRequestTimeout(2000);
        httpRequestFactory.setConnectTimeout(10000);

        RestTemplate restTemplate = new RestTemplate(httpRequestFactory);
        restTemplate.getMessageConverters().set(1,
                new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}
