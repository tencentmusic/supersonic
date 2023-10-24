package com.tencent.supersonic.common.interceptor;

import com.tencent.supersonic.common.util.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class LogInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //use previous traceId
        String traceId = request.getHeader(TraceIdUtil.TRACE_ID);
        if (StringUtils.isBlank(traceId)) {
            TraceIdUtil.setTraceId(TraceIdUtil.generateTraceId());
        } else {
            TraceIdUtil.setTraceId(traceId);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView)
            throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        //remove after Completing
        TraceIdUtil.remove();
    }
}
