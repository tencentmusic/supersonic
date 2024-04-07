package com.tencent.supersonic.headless;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.server.service.DomainService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.util.DataUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.stream.Collectors;

public class SchemaAuthTest extends BaseTest {

    @Autowired
    private DomainService domainService;

    @Autowired
    private DataSetService viewService;

    @Autowired
    private ModelService modelService;

    @Test
    public void test_getDomainList_alice() {
        User user = DataUtils.getUserAlice();
        List<DomainResp> domainResps = domainService.getDomainListWithAdminAuth(user);
        List<Long> expectedDomainIds = Lists.newArrayList(1L, 2L);
        Assertions.assertEquals(expectedDomainIds,
                domainResps.stream().map(DomainResp::getId).collect(Collectors.toList()));
    }

    @Test
    public void test_getModelList_alice() {
        User user = DataUtils.getUserAlice();
        List<ModelResp> modelResps = modelService.getModelListWithAuth(user, null, AuthType.ADMIN);
        List<Long> expectedModelIds = Lists.newArrayList(1L, 4L);
        Assertions.assertEquals(expectedModelIds,
                modelResps.stream().map(ModelResp::getId).collect(Collectors.toList()));
    }

    @Test
    public void test_getVisibleModelList_alice() {
        User user = DataUtils.getUserAlice();
        List<ModelResp> modelResps = modelService.getModelListWithAuth(user, null, AuthType.VISIBLE);
        List<Long> expectedModelIds = Lists.newArrayList(1L, 4L);
        Assertions.assertEquals(expectedModelIds,
                modelResps.stream().map(ModelResp::getId).collect(Collectors.toList()));
    }

    @Test
    public void test_getViewList_alice() {
        User user = DataUtils.getUserAlice();
        List<DataSetResp> modelResps = viewService.getDataSetsInheritAuth(user, 0L);
        List<Long> expectedViewIds = Lists.newArrayList(2L);
        Assertions.assertEquals(expectedViewIds,
                modelResps.stream().map(DataSetResp::getId).collect(Collectors.toList()));
    }

    @Test
    public void test_getDomainList_jack() {
        User user = DataUtils.getUserJack();
        List<DomainResp> domainResps = domainService.getDomainListWithAdminAuth(user);
        List<Long> expectedDomainIds = Lists.newArrayList(1L, 2L);
        Assertions.assertEquals(expectedDomainIds,
                domainResps.stream().map(DomainResp::getId).collect(Collectors.toList()));
    }

    @Test
    public void test_getModelList_jack() {
        User user = DataUtils.getUserJack();
        List<ModelResp> modelResps = modelService.getModelListWithAuth(user, null, AuthType.ADMIN);
        List<Long> expectedModelIds = Lists.newArrayList(1L, 2L, 3L);
        Assertions.assertEquals(expectedModelIds,
                modelResps.stream().map(ModelResp::getId).collect(Collectors.toList()));
    }

    @Test
    public void test_getViewList_jack() {
        User user = DataUtils.getUserJack();
        List<DataSetResp> modelResps = viewService.getDataSetsInheritAuth(user, 0L);
        List<Long> expectedViewIds = Lists.newArrayList(1L, 2L);
        Assertions.assertEquals(expectedViewIds,
                modelResps.stream().map(DataSetResp::getId).collect(Collectors.toList()));
    }

}
