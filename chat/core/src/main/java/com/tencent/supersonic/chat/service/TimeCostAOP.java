package com.tencent.supersonic.chat.service;

import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Aspect
public class TimeCostAOP {

    @Pointcut("@annotation(com.tencent.supersonic.chat.service.TimeCost)")
    private void timeCostAdvicePointcut() {

    }

    @Around("timeCostAdvicePointcut()")
    public Object timeCostAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("begin to add time cost!");
        Long startTime = System.currentTimeMillis();
        Object object = joinPoint.proceed();
        if (object instanceof QueryResult) {
            QueryResult queryResult = (QueryResult) object;
            queryResult.setQueryTimeCost(System.currentTimeMillis() - startTime);
            return queryResult;
        }
        return object;
    }
}
