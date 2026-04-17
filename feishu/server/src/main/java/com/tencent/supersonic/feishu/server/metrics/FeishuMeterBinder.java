package com.tencent.supersonic.feishu.server.metrics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.common.metrics.AbstractMeterBinder;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuUserMappingDO;
import com.tencent.supersonic.feishu.server.persistence.mapper.FeishuUserMappingMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Feishu module MeterBinder: registers Gauge for pending user mappings and pre-creates manual
 * counters for rate limiting and executor rejection. Called by FeishuBotService.
 */
@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@Component
public class FeishuMeterBinder extends AbstractMeterBinder {

    private final FeishuUserMappingMapper userMappingMapper;
    private Counter rateLimitCounter;
    private Counter rejectionCounter;

    public FeishuMeterBinder(FeishuUserMappingMapper userMappingMapper) {
        super(Tags.of("module", "feishu"));
        this.userMappingMapper = userMappingMapper;
    }

    @Override
    protected void doBindTo(MeterRegistry registry) {
        rateLimitCounter =
                Counter.builder("feishu.rate_limit.hits").tags(commonTags()).register(registry);
        rejectionCounter =
                Counter.builder("feishu.executor.rejections").tags(commonTags()).register(registry);
        Gauge.builder("feishu.mapping.pending", userMappingMapper, this::countPending)
                .tags(commonTags()).strongReference(true).register(registry);
    }

    public void incrementRateLimitHit() {
        if (rateLimitCounter != null) {
            rateLimitCounter.increment();
        }
    }

    public void incrementExecutorRejection() {
        if (rejectionCounter != null) {
            rejectionCounter.increment();
        }
    }

    private double countPending(FeishuUserMappingMapper mapper) {
        return mapper.selectCount(new LambdaQueryWrapper<FeishuUserMappingDO>()
                .eq(FeishuUserMappingDO::getStatus, 0));
    }
}
