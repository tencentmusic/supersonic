package com.tencent.supersonic.semantic.model.application;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.semantic.api.model.enums.DimensionTypeEnum;
import com.tencent.supersonic.semantic.api.model.enums.IdentifyTypeEnum;
import com.tencent.supersonic.semantic.api.model.pojo.DatasourceDetail;
import com.tencent.supersonic.semantic.api.model.pojo.Dim;
import com.tencent.supersonic.semantic.api.model.pojo.DimensionTimeTypeParams;
import com.tencent.supersonic.semantic.api.model.pojo.Identify;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.request.DatasourceReq;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.model.domain.DatabaseService;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.dataobject.DatasourceDO;
import com.tencent.supersonic.semantic.model.domain.repository.DatasourceRepository;
import com.tencent.supersonic.semantic.model.domain.repository.DateInfoRepository;
import com.tencent.supersonic.semantic.model.domain.utils.DatasourceConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.Mockito.when;

class DatasourceServiceImplTest {

    @Test
    void createDatasource() throws Exception {
        MetricService metricService = Mockito.mock(MetricService.class);
        DimensionService dimensionService = Mockito.mock(DimensionService.class);
        DatasourceRepository datasourceRepository = Mockito.mock(DatasourceRepository.class);
        DatabaseService databaseService = Mockito.mock(DatabaseService.class);
        DateInfoRepository dateInfoRepository = Mockito.mock(DateInfoRepository.class);
        DatasourceServiceImpl datasourceService = new DatasourceServiceImpl(datasourceRepository, databaseService,
                dimensionService, metricService, dateInfoRepository);
        DatasourceResp actualDatasourceResp = datasourceService.createDatasource(
                mockDatasourceReq(), User.getFakeUser());
        DatasourceResp expectedDatasourceResp = buildExpectedDatasourceResp();
        Assertions.assertEquals(expectedDatasourceResp, actualDatasourceResp);
    }

    @Test
    void updateDatasource() throws Exception {
        MetricService metricService = Mockito.mock(MetricService.class);
        DimensionService dimensionService = Mockito.mock(DimensionService.class);
        DatasourceRepository datasourceRepository = Mockito.mock(DatasourceRepository.class);
        DatabaseService databaseService = Mockito.mock(DatabaseService.class);
        DateInfoRepository dateInfoRepository = Mockito.mock(DateInfoRepository.class);
        DatasourceServiceImpl datasourceService = new DatasourceServiceImpl(datasourceRepository, databaseService,
                dimensionService, metricService, dateInfoRepository);
        DatasourceReq datasourceReq = mockDatasourceReq_update();
        DatasourceDO datasourceDO = DatasourceConverter.convert(mockDatasourceReq(), User.getFakeUser());
        when(datasourceRepository.getDatasourceById(datasourceReq.getId())).thenReturn(datasourceDO);
        User user = User.getFakeUser();
        user.setName("alice");
        DatasourceResp actualDatasourceResp = datasourceService.updateDatasource(mockDatasourceReq_update(), user);
        DatasourceResp expectedDatasourceResp = buildExpectedDatasourceResp_update();
        Assertions.assertEquals(expectedDatasourceResp, actualDatasourceResp);
        Assertions.assertEquals("admin", actualDatasourceResp.getCreatedBy());
        Assertions.assertEquals("alice", actualDatasourceResp.getUpdatedBy());
    }


    private DatasourceReq mockDatasourceReq() {
        DatasourceReq datasourceReq = new DatasourceReq();
        datasourceReq.setName("PVUV统计");
        datasourceReq.setBizName("s2_pv_uv_statis");
        datasourceReq.setDescription("PVUV统计");
        datasourceReq.setDatabaseId(1L);

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户名", IdentifyTypeEnum.primary.name(), "user_name"));
        datasourceReq.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionTypeEnum.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("", "page", DimensionTypeEnum.categorical.name(), 0);
        dimension2.setExpr("page");
        dimensions.add(dimension2);
        datasourceReq.setDimensions(dimensions);

        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("访问次数", "pv", AggOperatorEnum.SUM.name(), 1);
        measures.add(measure1);

        Measure measure2 = new Measure("访问人数", "uv", AggOperatorEnum.COUNT_DISTINCT.name(), 1);
        measures.add(measure2);

        datasourceReq.setMeasures(measures);
        datasourceReq.setSqlQuery("SELECT imp_date, user_name, page, 1 as pv, user_name as uv FROM s2_pv_uv_statis");
        datasourceReq.setQueryType("sql_query");
        datasourceReq.setModelId(1L);
        datasourceReq.setFilterSql("where user_name = 'alice'");
        return datasourceReq;
    }

    private DatasourceReq mockDatasourceReq_update() {
        DatasourceReq datasourceReq = new DatasourceReq();
        datasourceReq.setId(1L);
        datasourceReq.setName("PVUV统计_a");
        datasourceReq.setBizName("s2_pv_uv_statis_a");
        datasourceReq.setDescription("PVUV统计_a");
        datasourceReq.setDatabaseId(2L);

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户名_a", IdentifyTypeEnum.primary.name(), "user_name_a"));
        datasourceReq.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date_a", DimensionTypeEnum.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("", "page_a", DimensionTypeEnum.categorical.name(), 0);
        dimension2.setExpr("page_a");
        dimensions.add(dimension2);
        datasourceReq.setDimensions(dimensions);

        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("访问次数_a", "pv_a", AggOperatorEnum.SUM.name(), 1);
        measures.add(measure1);

        Measure measure2 = new Measure("访问人数_a", "uv_a", AggOperatorEnum.COUNT_DISTINCT.name(), 1);
        measures.add(measure2);

        datasourceReq.setMeasures(measures);
        datasourceReq.setSqlQuery("SELECT imp_date_a, user_name_a, page_a, 1 as pv_a, user_name "
                + "as uv_a FROM s2_pv_uv_statis");
        datasourceReq.setQueryType("sql_query");
        datasourceReq.setModelId(1L);
        datasourceReq.setFilterSql("where user_name = 'tom'");
        return datasourceReq;
    }

    private DatasourceResp buildExpectedDatasourceResp() {
        DatasourceResp datasourceResp = new DatasourceResp();
        datasourceResp.setName("PVUV统计");
        datasourceResp.setBizName("s2_pv_uv_statis");
        datasourceResp.setDescription("PVUV统计");
        datasourceResp.setDatabaseId(1L);
        DatasourceDetail datasourceDetail = new DatasourceDetail();
        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户名", IdentifyTypeEnum.primary.name(), "user_name"));
        datasourceDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionTypeEnum.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("", "page", DimensionTypeEnum.categorical.name(), 0);
        dimension2.setExpr("page");
        dimensions.add(dimension2);
        datasourceDetail.setDimensions(dimensions);

        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("访问次数", "s2_pv_uv_statis_pv", AggOperatorEnum.SUM.name(), 1);
        measure1.setExpr("pv");
        measures.add(measure1);

        Measure measure2 = new Measure("访问人数", "s2_pv_uv_statis_uv", AggOperatorEnum.COUNT_DISTINCT.name(), 1);
        measure2.setExpr("uv");
        measures.add(measure2);

        datasourceDetail.setMeasures(measures);
        datasourceDetail.setSqlQuery("SELECT imp_date, user_name, page, 1 as pv, user_name as uv FROM s2_pv_uv_statis");
        datasourceDetail.setQueryType("sql_query");
        datasourceResp.setDatasourceDetail(datasourceDetail);
        datasourceResp.setModelId(1L);
        datasourceResp.setFilterSql("where user_name = 'alice'");
        return datasourceResp;
    }

    private DatasourceResp buildExpectedDatasourceResp_update() {
        DatasourceResp datasourceResp = new DatasourceResp();
        datasourceResp.setName("PVUV统计_a");
        datasourceResp.setBizName("s2_pv_uv_statis_a");
        datasourceResp.setDescription("PVUV统计_a");
        datasourceResp.setDatabaseId(2L);
        DatasourceDetail datasourceDetail = new DatasourceDetail();
        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户名_a", IdentifyTypeEnum.primary.name(), "user_name_a"));
        datasourceDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date_a", DimensionTypeEnum.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("", "page_a", DimensionTypeEnum.categorical.name(), 0);
        dimension2.setExpr("page_a");
        dimensions.add(dimension2);
        datasourceDetail.setDimensions(dimensions);

        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("访问次数_a", "s2_pv_uv_statis_a_pv_a",
                AggOperatorEnum.SUM.name(), 1);
        measure1.setExpr("pv_a");
        measures.add(measure1);

        Measure measure2 = new Measure("访问人数_a", "s2_pv_uv_statis_a_uv_a",
                AggOperatorEnum.COUNT_DISTINCT.name(), 1);
        measure2.setExpr("uv_a");
        measures.add(measure2);

        datasourceDetail.setMeasures(measures);
        datasourceDetail.setSqlQuery("SELECT imp_date_a, user_name_a, page_a, 1 as pv_a, "
                + "user_name as uv_a FROM s2_pv_uv_statis");
        datasourceDetail.setQueryType("sql_query");
        datasourceResp.setDatasourceDetail(datasourceDetail);
        datasourceResp.setModelId(1L);
        datasourceResp.setFilterSql("where user_name = 'tom'");
        return datasourceResp;
    }

}