package com.tencent.supersonic;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthGroup;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRule;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.semantic.api.model.enums.DataTypeEnum;
import com.tencent.supersonic.semantic.api.model.enums.DimensionTypeEnum;
import com.tencent.supersonic.semantic.api.model.enums.IdentifyTypeEnum;
import com.tencent.supersonic.semantic.api.model.enums.SemanticTypeEnum;
import com.tencent.supersonic.semantic.api.model.pojo.Dim;
import com.tencent.supersonic.semantic.api.model.pojo.DimensionTimeTypeParams;
import com.tencent.supersonic.semantic.api.model.pojo.Entity;
import com.tencent.supersonic.semantic.api.model.pojo.Identify;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.model.request.DatabaseReq;
import com.tencent.supersonic.semantic.api.model.request.DatasourceReq;
import com.tencent.supersonic.semantic.api.model.request.DimensionReq;
import com.tencent.supersonic.semantic.api.model.request.DomainReq;
import com.tencent.supersonic.semantic.api.model.request.MetricReq;
import com.tencent.supersonic.semantic.api.model.request.ModelReq;
import com.tencent.supersonic.semantic.model.domain.DatabaseService;
import com.tencent.supersonic.semantic.model.domain.DatasourceService;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.DomainService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@Order(1)
public class LoadModelDataDemo implements CommandLineRunner {

    private User user = User.getFakeUser();

    @Value("${spring.h2.demo.enabled:false}")
    private boolean demoEnable;

    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private DomainService domainService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private DatasourceService datasourceService;
    @Autowired
    private DimensionService dimensionService;
    @Autowired
    private MetricService metricService;
    @Autowired
    private AuthService authService;

    @Override
    public void run(String... args) {
        if (!demoEnable) {
            return;
        }
        try {
            addDatabase();
            addDomain();
            addModel_1();
            addDatasource_1();
            addDatasource_2();
            addDatasource_3();
            addModel_2();
            addDatasource_4();
            updateDimension();
            updateMetric();
            addAuthGroup_1();
            addAuthGroup_2();
        } catch (Exception e) {
            log.error("Failed to add model demo data", e);
        }

    }

    public void addDatabase() {
        DatabaseReq databaseReq = new DatabaseReq();
        databaseReq.setName("H2数据实例");
        databaseReq.setDescription("样例数据库实例");
        databaseReq.setType(DataTypeEnum.H2.getFeature());
        databaseReq.setUrl("jdbc:h2:mem:semantic;DATABASE_TO_UPPER=false");
        databaseReq.setUsername("root");
        databaseReq.setPassword("semantic");
        databaseService.createOrUpdateDatabase(databaseReq, user);
    }

    public void addDomain() {
        DomainReq domainReq = new DomainReq();
        domainReq.setName("超音数");
        domainReq.setBizName("supersonic");
        domainReq.setParentId(0L);
        domainReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        domainReq.setViewOrgs(Collections.singletonList("admin"));
        domainReq.setAdmins(Collections.singletonList("admin"));
        domainReq.setAdminOrgs(Collections.emptyList());
        domainService.createDomain(domainReq, user);
    }

    public void addModel_1() {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("超音数");
        modelReq.setBizName("supersonic");
        modelReq.setDomainId(1L);
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("admin"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        modelService.createModel(modelReq, user);
    }

    public void addDatasource_1() throws Exception {
        DatasourceReq datasourceReq = new DatasourceReq();
        datasourceReq.setName("用户部门");
        datasourceReq.setBizName("user_department");
        datasourceReq.setDescription("用户部门");
        datasourceReq.setDatabaseId(1L);

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户名", IdentifyTypeEnum.primary.name(), "user_name"));
        datasourceReq.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        dimensions.add(new Dim("部门", "department",
                DimensionTypeEnum.categorical.name(), 1));
        datasourceReq.setDimensions(dimensions);

        datasourceReq.setMeasures(Collections.emptyList());
        datasourceReq.setQueryType("table_query");
        datasourceReq.setTableQuery("PUBLIC.s2_user_department");
        datasourceReq.setModelId(1L);
        datasourceService.createDatasource(datasourceReq, user);
    }

    public void addDatasource_2() throws Exception {
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
        datasourceService.createDatasource(datasourceReq, user);
    }

    public void addDatasource_3() throws Exception {
        DatasourceReq datasourceReq = new DatasourceReq();
        datasourceReq.setName("停留时长统计");
        datasourceReq.setBizName("s2_stay_time_statis");
        datasourceReq.setDescription("停留时长统计");
        datasourceReq.setDatabaseId(1L);

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户名", IdentifyTypeEnum.primary.name(), "user_name"));
        datasourceReq.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionTypeEnum.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("页面", "page", DimensionTypeEnum.categorical.name(), 1);
        dimension2.setExpr("page");
        dimensions.add(dimension2);
        datasourceReq.setDimensions(dimensions);

        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("停留时长", "stay_hours", AggregateTypeEnum.SUM.name(), 1);
        measures.add(measure1);

        datasourceReq.setMeasures(measures);
        datasourceReq.setTableQuery("PUBLIC.s2_stay_time_statis");
        datasourceReq.setQueryType("table_query");
        datasourceReq.setModelId(1L);
        datasourceService.createDatasource(datasourceReq, user);
    }

    public void addModel_2() {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("艺人库");
        modelReq.setBizName("singer");
        modelReq.setDomainId(1L);
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("admin"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        modelReq.setEntity(new Entity(7L, Arrays.asList("歌手", "艺人")));
        modelService.createModel(modelReq, user);
    }

    public void addDatasource_4() throws Exception {
        DatasourceReq datasourceReq = new DatasourceReq();
        datasourceReq.setName("艺人库");
        datasourceReq.setBizName("singer");
        datasourceReq.setDescription("艺人库");
        datasourceReq.setDatabaseId(1L);

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("歌手名", IdentifyTypeEnum.primary.name(), "singer_name"));
        datasourceReq.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionTypeEnum.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        dimensions.add(new Dim("活跃区域", "act_area",
                DimensionTypeEnum.categorical.name(), 1));
        dimensions.add(new Dim("代表作", "song_name",
                DimensionTypeEnum.categorical.name(), 1));
        dimensions.add(new Dim("风格", "genre",
                DimensionTypeEnum.categorical.name(), 1));
        datasourceReq.setDimensions(dimensions);

        Measure measure1 = new Measure("播放量", "js_play_cnt", "sum", 1);
        Measure measure2 = new Measure("下载量", "down_cnt", "sum", 1);
        Measure measure3 = new Measure("收藏量", "favor_cnt", "sum", 1);
        datasourceReq.setMeasures(Lists.newArrayList(measure1, measure2, measure3));
        datasourceReq.setQueryType("table_query");
        datasourceReq.setTableQuery("PUBLIC.singer");
        datasourceReq.setModelId(2L);
        datasourceService.createDatasource(datasourceReq, user);
    }

    public void updateDimension() throws Exception {
        DimensionReq dimensionReq = new DimensionReq();
        dimensionReq.setModelId(1L);
        dimensionReq.setType(DimensionTypeEnum.categorical.name());
        dimensionReq.setId(3L);
        dimensionReq.setName("页面");
        dimensionReq.setBizName("page");
        dimensionReq.setDatasourceId(3L);
        dimensionReq.setAlias("page");
        dimensionReq.setSemanticType(SemanticTypeEnum.CATEGORY.name());
        dimensionReq.setSensitiveLevel(2);
        dimensionReq.setDescription("页面");
        dimensionReq.setExpr("page");
        dimensionReq.setDimValueMaps(Collections.emptyList());
        dimensionService.updateDimension(dimensionReq, user);
    }

    public void updateMetric() throws Exception {
        MetricReq metricReq = new MetricReq();
        metricReq.setModelId(1L);
        metricReq.setId(3L);
        metricReq.setName("停留时长");
        metricReq.setBizName("stay_hours");
        metricReq.setSensitiveLevel(SensitiveLevelEnum.HIGH.getCode());
        metricReq.setDescription("停留时长");
        metricReq.setTags(Collections.singletonList("核心指标"));
        metricReq.setAlias("访问时长");
        MetricTypeParams metricTypeParams = new MetricTypeParams();
        metricTypeParams.setExpr("s2_stay_time_statis_stay_hours");
        List<Measure> measures = new ArrayList<>();
        Measure measure = new Measure("停留时长",
                "s2_stay_time_statis_stay_hours", AggOperatorEnum.SUM.getOperator(), 1);
        measure.setDatasourceId(3L);
        measures.add(measure);
        metricTypeParams.setMeasures(measures);
        metricReq.setTypeParams(metricTypeParams);
        metricService.updateExprMetric(metricReq, user);
    }

    public void addAuthGroup_1() {
        AuthGroup authGroupReq = new AuthGroup();
        authGroupReq.setModelId("1");
        authGroupReq.setName("admin-permission");

        List<AuthRule> authRules = new ArrayList<>();
        AuthRule authRule = new AuthRule();
        authRule.setMetrics(Collections.singletonList("stay_hours"));
        authRule.setDimensions(Collections.singletonList("page"));
        authRules.add(authRule);

        authGroupReq.setAuthRules(authRules);
        authGroupReq.setAuthorizedUsers(Collections.singletonList("jack"));
        authGroupReq.setAuthorizedDepartmentIds(Collections.emptyList());
        authService.addOrUpdateAuthGroup(authGroupReq);
    }

    public void addAuthGroup_2() {
        AuthGroup authGroupReq = new AuthGroup();
        authGroupReq.setModelId("1");
        authGroupReq.setName("tom_sales_permission");

        List<AuthRule> authRules = new ArrayList<>();
        AuthRule authRule = new AuthRule();
        authRule.setMetrics(Collections.singletonList("stay_hours"));
        authRule.setDimensions(Collections.singletonList("page"));
        authRules.add(authRule);

        authGroupReq.setAuthRules(authRules);
        authGroupReq.setDimensionFilters(Collections.singletonList("department in ('sales')"));
        authGroupReq.setDimensionFilterDescription("部门 in [sales]");
        authGroupReq.setAuthorizedUsers(Collections.singletonList("tom"));
        authGroupReq.setAuthorizedDepartmentIds(Collections.emptyList());
        authService.addOrUpdateAuthGroup(authGroupReq);
    }

}