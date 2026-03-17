package com.tencent.supersonic.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.boot.web.server.ErrorPageRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/** error page config */
@Component
public class ErrorPageConfig implements ErrorPageRegistrar, HandlerExceptionResolver, Ordered {

    private static final String INDEX_PATH = "/webapp/index.html";
    private static final boolean INDEX_EXISTS = new ClassPathResource("webapp/index.html").exists();

    @Override
    public void registerErrorPages(ErrorPageRegistry registry) {
        if (INDEX_EXISTS) {
            ErrorPage error404Page = new ErrorPage(HttpStatus.NOT_FOUND, INDEX_PATH);
            registry.addErrorPages(error404Page);
        }
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
            // Avoid circular forward when index.html doesn't exist or request is already
            // for index.html
            String uri = request.getRequestURI();
            if (!INDEX_EXISTS || uri.endsWith("/index.html")) {
                return null;
            }
            ModelAndView modelAndView = new ModelAndView(INDEX_PATH);
            response.setStatus(HttpStatus.OK.value());
            return modelAndView;
        }

        return null;
    }
}
