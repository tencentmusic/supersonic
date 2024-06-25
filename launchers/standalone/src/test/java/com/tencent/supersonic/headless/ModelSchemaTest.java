package com.tencent.supersonic.headless;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.pojo.request.FieldRemovedReq;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.UnAvailableItemResp;
import com.tencent.supersonic.headless.server.web.service.ModelService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ModelSchemaTest extends BaseTest {

    @Autowired
    private ModelService modelService;

    @Test
    void testGetUnAvailableItem() {
        FieldRemovedReq fieldRemovedReq = new FieldRemovedReq();
        fieldRemovedReq.setModelId(2L);
        fieldRemovedReq.setFields(Lists.newArrayList("pv"));
        UnAvailableItemResp unAvailableItemResp = modelService.getUnAvailableItem(fieldRemovedReq);
        List<Long> expectedUnAvailableMetricId = Lists.newArrayList(1L, 4L);
        List<Long> actualUnAvailableMetricId = unAvailableItemResp.getMetricResps()
                .stream().map(MetricResp::getId).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        Assertions.assertEquals(expectedUnAvailableMetricId, actualUnAvailableMetricId);
    }

}
