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
 * 飞书模块指标切面：对消息处理器耗时与消息发送次数进行埋点。
 * <p>
 * 仅在存在 {@link MeterRegistry} 时生效（{@link ConditionalOnBean}），
 * 用于 Prometheus/Actuator 采集：处理器耗时分布、消息发送成功/失败计数。
 * MeterRegistry 通过构造器注入，无需空校验。
 */
@Aspect
@Component
@ConditionalOnBean(MeterRegistry.class)
public class FeishuMetricsAspect {

    /** 所有飞书指标的通用 tag，便于在 Prometheus 中按 module=feishu 过滤。 */
    private static final Tags FEISHU_TAGS = Tags.of("module", "feishu");

    private final MeterRegistry registry;

    /**
     * 注入 Micrometer 注册表，用于创建 Timer/Counter。
     *
     * @param registry 由 Spring Boot 自动配置的 MeterRegistry（如 PrometheusMeterRegistry）
     */
    public FeishuMetricsAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 环绕切面：统计各 {@link com.tencent.supersonic.feishu.server.handler.MessageHandler#handle}
     * 的执行耗时。指标名：{@code feishu.query.duration}；标签：handler=类名、status=success|error。
     *
     * @param pjp 被拦截的 handle 方法
     * @return 原方法返回值
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

    /**
     * 获取或创建「查询耗时」计时器。同一 handler+status 复用同一 Timer 实例。
     *
     * @param handler Handler 类简单名（如 QueryMessageHandler）
     * @param status  成功为 success，异常为 error
     * @return feishu.query.duration 对应的 Timer
     */
    private Timer queryTimer(String handler, String status) {
        return Timer.builder("feishu.query.duration").tags(FEISHU_TAGS).tag("handler", handler)
                .tag("status", status).register(registry);
    }

    /**
     * 消息发送成功后的后置切面：递增 {@code feishu.message.sent} 计数。
     * 切点：{@link com.tencent.supersonic.feishu.server.service.FeishuMessageSender} 的 reply*、send*、uploadFile。
     * 标签 type：根据方法名推断，text/card/file。
     */
    @AfterReturning("execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.reply*(..)) || "
            + "execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.send*(..)) || "
            + "execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.uploadFile(..))")
    public void afterMessageSent(JoinPoint jp) {
        Counter.builder("feishu.message.sent").tags(FEISHU_TAGS).tag("type", extractType(jp))
                .register(registry).increment();
    }

    /**
     * 消息发送异常后的切面：递增 {@code feishu.message.send.errors} 计数。
     * 切点与 {@link #afterMessageSent} 相同；标签 type 同样为 text/card/file。
     */
    @AfterThrowing("execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.reply*(..)) || "
            + "execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.send*(..)) || "
            + "execution(* com.tencent.supersonic.feishu.server.service.FeishuMessageSender.uploadFile(..))")
    public void afterMessageError(JoinPoint jp) {
        Counter.builder("feishu.message.send.errors").tags(FEISHU_TAGS).tag("type", extractType(jp))
                .register(registry).increment();
    }

    /**
     * 根据 FeishuMessageSender 方法名推断消息类型，用于 metric 的 type 标签。
     * 规则：方法名含 Text -> text，含 Card -> card，否则 -> file（如 uploadFile、sendFile）。
     *
     * @param jp 当前切点（reply/send/upload 方法）
     * @return "text" | "card" | "file"
     */
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
