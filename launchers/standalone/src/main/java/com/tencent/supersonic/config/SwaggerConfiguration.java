package com.tencent.supersonic.config;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.config.AuthenticationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@EnableOpenApi
public class SwaggerConfiguration {

    /**
     * 标题
     */
    @Value("${swagger.title}")
    private String title;

    /**
     * 基本包
     */
    @Value("${swagger.base.package}")
    private String basePackage;

    /**
     * 描述
     */
    @Value("${swagger.description}")
    private String description;

    /**
     * URL
     */
    @Value("${swagger.url}")
    private String url;

    /**
     * 作者
     */
    @Value("${swagger.contact.name}")
    private String contactName;

    /**
     * 作者网址
     */
    @Value("${swagger.contact.url}")
    private String contactUrl;

    /**
     * 作者邮箱
     */
    @Value("${swagger.contact.email}")
    private String contactEmail;

    /**
     * 版本
     */
    @Value("${swagger.version}")
    private String version;

    @Autowired
    private AuthenticationConfig authenticationConfig;

    @Bean
    public Docket createRestApi() {

        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage(basePackage))
                .paths(PathSelectors.any())
                .build().securitySchemes(Lists.newArrayList(apiKey()));

    }

    private ApiKey apiKey() {
        return new ApiKey(authenticationConfig.getTokenHttpHeaderKey(),
                authenticationConfig.getTokenHttpHeaderKey(), "header");
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title(title)
                .description(description)
                .termsOfServiceUrl(url)
                .contact(new Contact(contactName, contactUrl, contactEmail))
                .version(version)
                .build();
    }
}