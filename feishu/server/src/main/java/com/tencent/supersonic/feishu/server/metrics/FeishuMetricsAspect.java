package com.tencent.supersonic.feishu.server.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Pure AOP aspect for feishu module metrics: handler timing and message send counting.
 * MeterRegistry is constructor-injected — no null guards needed.
 */
@Aspect
@Component
@ConditionalOnBean(MeterRegistry.class)
public class FeishuMetricsAspect {

    private static final Tags FEISHU_TAGS = Tags.of("module", "feishu");

    private final MeterRegistry registry;

    public FeishuMetricsAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Times {@code MessageHandler.handle()} execution. Tags: handler class name, status
     * (success/error).
     */
    @Around("execution(* com.tencent.supersonic.feishu.server.handler.MessageHandler+.handle(..))")
    public Object aroundHandle(ProceedingJoinPoint pjp) throws Throwable {
        Timer.Sample sample = Timer.start(registry);
        String handler = pjp.getTarget().getClass().getSimpleName();
        try {
            Object result = pjp.proceed();
            sample.stop(queryTimer(handler, "success"));
            return result;
        } catch (Throwable t) {
            sample.stop(queryTimer(handler, "error"));
            throw t;
        }
    }

    /** Returns (or creates) {@code feishu.query.duration} timer for the given handler + status. */
    private Timer queryTimer(String handler, String status) {
        return Timer.builder("feishu.query.duration").tags(FEISHU_TAGS).tag("handler", handler)
                .tag("status", status).register(registry);
    }

    /** Increments {@code feishu.message.sent} on successful send. Tag type: text/card/file. */
    @AfterReturning("execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.reply*(..)) || "
            + "execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.send*(..)) || "
            + "execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.uploadFile(..))")
    public void afterMessageSent(JoinPoint jp) {
        Counter.builder("feishu.message.sent").tags(FEISHU_TAGS).tag("type", extractType(jp))
                .register(registry).increment();
    }

    /** Increments {@code feishu.message.send.errors} on send failure. Tag type: text/card/file. */
    @AfterThrowing("execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.reply*(..)) || "
            + "execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.send*(..)) || "
            + "execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.uploadFile(..))")
    public void afterMessageError(JoinPoint jp) {
        Counter.builder("feishu.message.send.errors").tags(FEISHU_TAGS).tag("type", extractType(jp))
                .register(registry).increment();
    }

    /** Infers message type from method name: *Text* -> text, *Card* -> card, else -> file. */
    private String extractType(JoinPoint jp) {
        String method = jp.getSignature().getName();
        if (method.contains("Text")) {
            return "text";
        }
        if (method.contains("Card")) {
            return "card";
        }
        return "file";
    }
}
