package com.tencent.supersonic.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tencent.supersonic.common.pojo.ResultData;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return !methodParameter.getDeclaringClass().isAssignableFrom(BasicErrorController.class);
    }

    @SneakyThrows
    @Override
    public Object beforeBodyWrite(Object result, MethodParameter methodParameter, MediaType mediaType,
                                  Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest,
                                  ServerHttpResponse serverHttpResponse) {
        // 判断当前请求是否是 Swagger 相关的请求
        String path = serverHttpRequest.getURI().getPath();
        if (path.startsWith("/swagger") || path.startsWith("/v3/api-docs") || path.startsWith("/v2/api-docs")) {
            return result;
        }
        objectMapper.registerModule(new JavaTimeModule());
        if (result instanceof String) {
            return objectMapper.writeValueAsString(ResultData.success(result));
        }
        if (result instanceof ResultData) {
            return result;
        }
        return ResultData.success(result);
    }
}