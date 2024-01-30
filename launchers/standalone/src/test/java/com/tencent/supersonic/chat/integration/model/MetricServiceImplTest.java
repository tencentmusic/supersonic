package com.tencent.supersonic.chat.integration.model;

import com.google.common.collect.Lists;
import com.tencent.supersonic.BaseApplication;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.headless.api.pojo.enums.MetricType;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.server.pojo.MetricFilter;
import com.tencent.supersonic.headless.server.service.MetricService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class MetricServiceImplTest extends BaseApplication {

    @Autowired
    protected MetricService metricService;

    @Test
    void getMetrics() {
        MetricFilter metricFilter = new MetricFilter();
        metricFilter.setType(MetricType.ATOMIC.name());
        metricFilter.setModelIds(Lists.newArrayList(1L));
        metricFilter.setSensitiveLevel(SensitiveLevelEnum.LOW.ordinal());
        List<MetricResp> metricResps = metricService.getMetrics(metricFilter);
        Assertions.assertTrue(metricResps.stream().noneMatch(metricResp -> metricResp.getModelId().equals(2L)));
        Assertions.assertTrue(metricResps.stream().noneMatch(metricResp ->
                metricResp.getSensitiveLevel().equals(SensitiveLevelEnum.HIGH.ordinal())));
    }

}