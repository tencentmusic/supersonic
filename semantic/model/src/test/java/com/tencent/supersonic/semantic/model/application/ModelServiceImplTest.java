package com.tencent.supersonic.semantic.model.application;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.semantic.api.model.enums.DimensionTypeEnum;
import com.tencent.supersonic.semantic.api.model.enums.IdentifyTypeEnum;
import com.tencent.supersonic.semantic.api.model.pojo.Dim;
import com.tencent.supersonic.semantic.api.model.pojo.DimensionTimeTypeParams;
import com.tencent.supersonic.semantic.api.model.pojo.Identify;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.pojo.ModelDetail;
import com.tencent.supersonic.semantic.api.model.request.ModelReq;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.model.domain.DatabaseService;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.DomainService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.ModelRelaService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.dataobject.ModelDO;
import com.tencent.supersonic.semantic.model.domain.repository.DateInfoRepository;
import com.tencent.supersonic.semantic.model.domain.repository.ModelRepository;
import com.tencent.supersonic.semantic.model.domain.utils.ModelConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

class ModelServiceImplTest {

    @Test
    void createModel() throws Exception {
        ModelRepository modelRepository = Mockito.mock(ModelRepository.class);
        ModelService modelService = mockModelService(modelRepository);
        ModelResp actualModelResp = modelService.createModel(
                mockModelReq(), User.getFakeUser());
        ModelResp expectedModelResp = buildExpectedModelResp();
        Assertions.assertEquals(expectedModelResp, actualModelResp);
    }

    @Test
    void updateModel() throws Exception {
        ModelRepository modelRepository = Mockito.mock(ModelRepository.class);
        ModelService modelService = mockModelService(modelRepository);
        ModelReq modelReq = mockModelReq_update();
        ModelDO modelDO = ModelConverter.convert(mockModelReq(), User.getFakeUser());
        when(modelRepository.getModelById(modelReq.getId())).thenReturn(modelDO);
        User user = User.getFakeUser();
        user.setName("alice");
        ModelResp actualModelResp = modelService.updateModel(modelReq, user);
        ModelResp expectedModelResp = buildExpectedModelResp_update();
        Assertions.assertEquals(expectedModelResp, actualModelResp);
        Assertions.assertEquals("admin", actualModelResp.getCreatedBy());
        Assertions.assertEquals("alice", actualModelResp.getUpdatedBy());
    }

    private ModelService mockModelService(ModelRepository modelRepository) {
        MetricService metricService = Mockito.mock(MetricService.class);
        DimensionService dimensionService = Mockito.mock(DimensionService.class);
        DatabaseService databaseService = Mockito.mock(DatabaseService.class);
        DomainService domainService = Mockito.mock(DomainService.class);
        UserService userService = Mockito.mock(UserService.class);
        ModelRelaService modelRelaService = Mockito.mock(ModelRelaService.class);
        DateInfoRepository dateInfoRepository = Mockito.mock(DateInfoRepository.class);
        return new ModelServiceImpl(modelRepository, databaseService,
                dimensionService, metricService, modelRelaService, domainService, userService, dateInfoRepository);
    }

    private ModelReq mockModelReq() {
        ModelReq modelReq = new ModelReq();
        modelReq.setId(1L);
        modelReq.setName("PVUV统计");
        modelReq.setBizName("s2_pv_uv_statis");
        modelReq.setDescription("PVUV统计");
        modelReq.setDatabaseId(1L);
        modelReq.setAlias("访问次数统计,PVUV统计");
        modelReq.setAdmins(Lists.newArrayList("admin", "tom"));
        modelReq.setViewers(Lists.newArrayList("alice", "lucy"));
        ModelDetail modelDetail = new ModelDetail();
        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户名", IdentifyTypeEnum.primary.name(), "user_name"));
        modelDetail.setIdentifiers(identifiers);
        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionTypeEnum.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("", "page", DimensionTypeEnum.categorical.name(), 0);
        dimension2.setExpr("page");
        dimensions.add(dimension2);
        modelDetail.setDimensions(dimensions);
        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("访问次数", "pv", AggOperatorEnum.SUM.name(), 1);
        measures.add(measure1);
        Measure measure2 = new Measure("访问人数", "uv", AggOperatorEnum.COUNT_DISTINCT.name(), 1);
        measures.add(measure2);
        modelDetail.setMeasures(measures);
        modelDetail.setSqlQuery("SELECT imp_date, user_name, page, 1 as pv, user_name as uv FROM s2_pv_uv_statis");
        modelDetail.setQueryType("sql_query");
        modelReq.setDomainId(1L);
        modelReq.setFilterSql("where user_name = 'alice'");
        modelReq.setModelDetail(modelDetail);
        return modelReq;
    }

    private ModelReq mockModelReq_update() {
        ModelReq modelReq = new ModelReq();
        modelReq.setId(1L);
        modelReq.setName("PVUV统计_a");
        modelReq.setBizName("s2_pv_uv_statis_a");
        modelReq.setDescription("PVUV统计_a");
        modelReq.setDatabaseId(2L);
        modelReq.setDomainId(1L);
        modelReq.setAlias("访问次数统计,PVUV统计");
        modelReq.setAdmins(Lists.newArrayList("admin"));
        modelReq.setViewers(Lists.newArrayList("alice"));
        ModelDetail modelDetail = new ModelDetail();
        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户名_a", IdentifyTypeEnum.primary.name(), "user_name_a"));
        modelDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date_a", DimensionTypeEnum.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("", "page_a", DimensionTypeEnum.categorical.name(), 0);
        dimension2.setExpr("page_a");
        dimensions.add(dimension2);
        modelDetail.setDimensions(dimensions);

        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("访问次数_a", "pv_a", AggOperatorEnum.SUM.name(), 1);
        measures.add(measure1);

        Measure measure2 = new Measure("访问人数_a", "uv_a", AggOperatorEnum.COUNT_DISTINCT.name(), 1);
        measures.add(measure2);

        modelDetail.setMeasures(measures);
        modelDetail.setSqlQuery("SELECT imp_date_a, user_name_a, page_a, 1 as pv_a, user_name "
                + "as uv_a FROM s2_pv_uv_statis");
        modelDetail.setQueryType("sql_query");
        modelReq.setDomainId(1L);
        modelReq.setFilterSql("where user_name = 'tom'");
        modelReq.setModelDetail(modelDetail);
        return modelReq;
    }

    private ModelResp buildExpectedModelResp() {
        ModelResp modelResp = new ModelResp();
        modelResp.setName("PVUV统计");
        modelResp.setBizName("s2_pv_uv_statis");
        modelResp.setDescription("PVUV统计");
        modelResp.setDatabaseId(1L);
        modelResp.setDomainId(1L);
        modelResp.setAlias("访问次数统计,PVUV统计");
        modelResp.setAdmins(Lists.newArrayList("admin", "tom"));
        modelResp.setViewers(Lists.newArrayList("alice", "lucy"));
        ModelDetail modelDetail = new ModelDetail();
        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户名", IdentifyTypeEnum.primary.name(), "user_name"));
        modelDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionTypeEnum.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("", "page", DimensionTypeEnum.categorical.name(), 0);
        dimension2.setExpr("page");
        dimensions.add(dimension2);
        modelDetail.setDimensions(dimensions);

        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("访问次数", "s2_pv_uv_statis_pv", AggOperatorEnum.SUM.name(), 1);
        measure1.setExpr("pv");
        measures.add(measure1);

        Measure measure2 = new Measure("访问人数", "s2_pv_uv_statis_uv", AggOperatorEnum.COUNT_DISTINCT.name(), 1);
        measure2.setExpr("uv");
        measures.add(measure2);

        modelDetail.setMeasures(measures);
        modelDetail.setSqlQuery("SELECT imp_date, user_name, page, 1 as pv, user_name as uv FROM s2_pv_uv_statis");
        modelDetail.setQueryType("sql_query");
        modelResp.setModelDetail(modelDetail);
        modelResp.setId(1L);
        modelResp.setFilterSql("where user_name = 'alice'");
        return modelResp;
    }

    private ModelResp buildExpectedModelResp_update() {
        ModelResp modelResp = new ModelResp();
        modelResp.setName("PVUV统计_a");
        modelResp.setBizName("s2_pv_uv_statis_a");
        modelResp.setDescription("PVUV统计_a");
        modelResp.setDatabaseId(2L);
        modelResp.setDomainId(1L);
        modelResp.setAlias("访问次数统计,PVUV统计");
        modelResp.setAdmins(Lists.newArrayList("admin"));
        modelResp.setViewers(Lists.newArrayList("alice"));
        ModelDetail modelDetail = new ModelDetail();
        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户名_a", IdentifyTypeEnum.primary.name(), "user_name_a"));
        modelDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date_a", DimensionTypeEnum.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("", "page_a", DimensionTypeEnum.categorical.name(), 0);
        dimension2.setExpr("page_a");
        dimensions.add(dimension2);
        modelDetail.setDimensions(dimensions);

        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("访问次数_a", "s2_pv_uv_statis_a_pv_a",
                AggOperatorEnum.SUM.name(), 1);
        measure1.setExpr("pv_a");
        measures.add(measure1);

        Measure measure2 = new Measure("访问人数_a", "s2_pv_uv_statis_a_uv_a",
                AggOperatorEnum.COUNT_DISTINCT.name(), 1);
        measure2.setExpr("uv_a");
        measures.add(measure2);

        modelDetail.setMeasures(measures);
        modelDetail.setSqlQuery("SELECT imp_date_a, user_name_a, page_a, 1 as pv_a, "
                + "user_name as uv_a FROM s2_pv_uv_statis");
        modelDetail.setQueryType("sql_query");
        modelResp.setModelDetail(modelDetail);
        modelResp.setId(1L);
        modelResp.setFilterSql("where user_name = 'tom'");
        return modelResp;
    }

}
