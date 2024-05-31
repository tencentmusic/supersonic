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
    private DataSetService dataSetService;

    @Autowired
    private ModelService modelService;

    @Test
    public void test_getDomainList_alice() {
        User user = DataUtils.getUserAlice();
        List<DomainResp> domainResps = domainService.getDomainListWithAdminAuth(user);
        List<String> expectedDomainBizNames = Lists.newArrayList("supersonic", "visit_info", "singer", "singer_info");
        Assertions.assertEquals(expectedDomainBizNames,
                domainResps.stream().map(DomainResp::getBizName).collect(Collectors.toList()));
    }

    @Test
    public void test_getModelList_alice() {
        User user = DataUtils.getUserAlice();
        List<ModelResp> modelResps = modelService.getModelListWithAuth(user, null, AuthType.ADMIN);
        List<String> expectedModelBizNames = Lists.newArrayList("user_department", "singer");
        Assertions.assertEquals(expectedModelBizNames,
                modelResps.stream().map(ModelResp::getBizName).collect(Collectors.toList()));
    }

    @Test
    public void test_getVisibleModelList_alice() {
        User user = DataUtils.getUserAlice();
        List<ModelResp> modelResps = modelService.getModelListWithAuth(user, null, AuthType.VISIBLE);
        List<String> expectedModelBizNames = Lists.newArrayList("user_department", "singer");
        Assertions.assertEquals(expectedModelBizNames,
                modelResps.stream().map(ModelResp::getBizName).collect(Collectors.toList()));
    }

    @Test
    public void test_getDataSetList_alice() {
        User user = DataUtils.getUserAlice();
        List<DataSetResp> dataSetResps = dataSetService.getDataSetsInheritAuth(user, 0L);
        List<String> expectedDataSetBizNames = Lists.newArrayList("singer");
        Assertions.assertEquals(expectedDataSetBizNames,
                dataSetResps.stream().map(DataSetResp::getBizName).collect(Collectors.toList()));
    }

    @Test
    public void test_getDomainList_jack() {
        User user = DataUtils.getUserJack();
        List<DomainResp> domainResps = domainService.getDomainListWithAdminAuth(user);
        List<String> expectedDomainBizNames = Lists.newArrayList("supersonic", "visit_info");
        Assertions.assertEquals(expectedDomainBizNames,
                domainResps.stream().map(DomainResp::getBizName).collect(Collectors.toList()));
    }

    @Test
    public void test_getModelList_jack() {
        User user = DataUtils.getUserJack();
        List<ModelResp> modelResps = modelService.getModelListWithAuth(user, null, AuthType.ADMIN);
        List<String> expectedModelBizNames = Lists.newArrayList("user_department",
                "s2_pv_uv_statis", "s2_stay_time_statis");
        Assertions.assertEquals(expectedModelBizNames,
                modelResps.stream().map(ModelResp::getBizName).collect(Collectors.toList()));
    }

    @Test
    public void test_getDataSetList_jack() {
        User user = DataUtils.getUserJack();
        List<DataSetResp> dataSetResps = dataSetService.getDataSetsInheritAuth(user, 0L);
        List<String> expectedDataSetBizNames = Lists.newArrayList("s2", "singer");
        Assertions.assertEquals(expectedDataSetBizNames,
                dataSetResps.stream().map(DataSetResp::getBizName).collect(Collectors.toList()));
    }

}
