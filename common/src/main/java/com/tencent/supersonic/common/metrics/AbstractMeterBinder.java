package com.tencent.supersonic.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

/**
 * Base class for {@link MeterBinder} implementations with common tags, guard condition, and error
 * handling. Inspired by microsphere-observability's AbstractMeterBinder.
 */
public abstract class AbstractMeterBinder implements MeterBinder {

    private final Iterable<Tag> tags;

    protected AbstractMeterBinder() {
        this.tags = Tags.empty();
    }

    protected AbstractMeterBinder(Iterable<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public final void bindTo(@NonNull MeterRegistry registry) {
        if (!supports(registry)) {
            return;
        }
        try {
            doBindTo(registry);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).warn("Failed to bind metrics: {}", e.getMessage(),
                    e);
        }
    }

    protected boolean supports(MeterRegistry registry) {
        return true;
    }

    protected abstract void doBindTo(MeterRegistry registry);

    /**
     * Returns common tags: constructor-provided tags + origin=subclass simple name.
     */
    protected Tags commonTags() {
        return Tags.of(tags).and("origin", getClass().getSimpleName());
    }
}
