package com.tencent.supersonic.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/webapp/**").addResourceLocations("classpath:/webapp/");

        registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/webapp/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("redirect:/webapp/");
        registry.addViewController("/webapp/").setViewName("forward:/webapp/index.html");
        registry.addViewController("/webapp/**/{path:[^\\.]*}")
                .setViewName("forward:/webapp/index.html");
    }
}
