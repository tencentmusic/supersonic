package com.tencent.supersonic.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/** error page config */
@Component
public class ErrorPageConfig implements ErrorPageRegistrar, HandlerExceptionResolver, Ordered {

    @Override
    public void registerErrorPages(ErrorPageRegistry registry) {
        ErrorPage error404Page = new ErrorPage(HttpStatus.NOT_FOUND, "/webapp/index.html");
        registry.addErrorPages(error404Page);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) {
        if (handler instanceof ResourceHttpRequestHandler
                && ex instanceof NoResourceFoundException) {
            ModelAndView modelAndView = new ModelAndView("/webapp/index.html");
            response.setStatus(HttpStatus.OK.value());
            return modelAndView;
        }

        return null;
    }
}
