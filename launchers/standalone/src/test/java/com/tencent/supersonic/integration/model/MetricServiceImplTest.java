package com.tencent.supersonic.integration.model;

import com.google.common.collect.Lists;
import com.tencent.supersonic.StandaloneLauncher;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.semantic.api.model.enums.MetricTypeEnum;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.pojo.MetricFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;

@SpringBootTest(classes = StandaloneLauncher.class)
@ActiveProfiles("local")
public class MetricServiceImplTest {

    @Autowired
    protected MetricService metricService;

    @Test
    void getMetrics() {
        MetricFilter metricFilter = new MetricFilter();
        metricFilter.setType(MetricTypeEnum.ATOMIC.name());
        metricFilter.setModelIds(Lists.newArrayList(1L));
        metricFilter.setSensitiveLevel(SensitiveLevelEnum.LOW.ordinal());
        List<MetricResp> metricResps = metricService.getMetrics(metricFilter);
        Assertions.assertTrue(metricResps.stream().noneMatch(metricResp -> metricResp.getModelId().equals(2L)));
        Assertions.assertTrue(metricResps.stream().noneMatch(metricResp ->
                metricResp.getSensitiveLevel().equals(SensitiveLevelEnum.HIGH.ordinal())));
    }

}