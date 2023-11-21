package com.tencent.supersonic;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthGroup;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRule;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.common.pojo.JoinCondition;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.semantic.api.model.enums.DataTypeEnum;
import com.tencent.supersonic.semantic.api.model.enums.DimensionTypeEnum;
import com.tencent.supersonic.semantic.api.model.enums.IdentifyTypeEnum;
import com.tencent.supersonic.semantic.api.model.enums.SemanticTypeEnum;
import com.tencent.supersonic.semantic.api.model.pojo.Dim;
import com.tencent.supersonic.semantic.api.model.pojo.DimensionTimeTypeParams;
import com.tencent.supersonic.semantic.api.model.pojo.Identify;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.pojo.MetricTypeParams;
import com.tencent.supersonic.semantic.api.model.pojo.ModelDetail;
import com.tencent.supersonic.semantic.api.model.request.DatabaseReq;
import com.tencent.supersonic.semantic.api.model.request.DimensionReq;
import com.tencent.supersonic.semantic.api.model.request.DomainReq;
import com.tencent.supersonic.semantic.api.model.request.MetricReq;
import com.tencent.supersonic.semantic.api.model.request.ModelReq;
import com.tencent.supersonic.semantic.model.domain.DatabaseService;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.DomainService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.ModelRelaService;
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
    private ModelRelaService modelRelaService;
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
            addModel_2();
            addModel_3();
            addModelRela_1();
            addModelRela_2();
            addDomain_2();
            addModel_4();
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
        domainReq.setStatus(StatusEnum.ONLINE.getCode());
        domainReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        domainReq.setViewOrgs(Collections.singletonList("admin"));
        domainReq.setAdmins(Collections.singletonList("admin"));
        domainReq.setAdminOrgs(Collections.emptyList());
        domainService.createDomain(domainReq, user);
    }

    public void addModel_1() throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("超音数用户部门");
        modelReq.setBizName("user_department");
        modelReq.setDescription("用户部门信息");
        modelReq.setDatabaseId(1L);
        modelReq.setDomainId(1L);
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("admin"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户", IdentifyTypeEnum.primary.name(), "user_name"));
        modelDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        dimensions.add(new Dim("部门", "department",
                DimensionTypeEnum.categorical.name(), 1));
        modelDetail.setDimensions(dimensions);

        modelDetail.setMeasures(Collections.emptyList());
        modelDetail.setQueryType("table_query");
        modelDetail.setSqlQuery("select user_name,department from PUBLIC.s2_user_department");
        modelReq.setModelDetail(modelDetail);
        modelReq.setDomainId(1L);
        modelService.createModel(modelReq, user);
    }

    public void addModel_2() throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("超音数PVUV统计");
        modelReq.setBizName("s2_pv_uv_statis");
        modelReq.setDescription("超音数PVUV统计");
        modelReq.setDatabaseId(1L);
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("admin"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        List<Identify> identifiers = new ArrayList<>();
        ModelDetail modelDetail = new ModelDetail();
        identifiers.add(new Identify("用户名", IdentifyTypeEnum.primary.name(), "s2_pv_uv_statis_user_name"));
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
        modelDetail.setSqlQuery("SELECT imp_date, user_name as s2_pv_uv_statis_user_name, page, 1 as pv, "
                + "user_name as uv FROM s2_pv_uv_statis");
        modelDetail.setQueryType("sql_query");
        modelReq.setDomainId(1L);
        modelReq.setModelDetail(modelDetail);
        modelService.createModel(modelReq, user);
    }

    public void addModel_3() throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("停留时长统计");
        modelReq.setBizName("s2_stay_time_statis");
        modelReq.setDescription("停留时长统计");
        modelReq.setDatabaseId(1L);
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("admin"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        List<Identify> identifiers = new ArrayList<>();
        ModelDetail modelDetail = new ModelDetail();
        identifiers.add(new Identify("用户名称", IdentifyTypeEnum.primary.name(), "stay_hours_user_name"));
        modelDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionTypeEnum.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("页面", "page", DimensionTypeEnum.categorical.name(), 1);
        dimension2.setExpr("page");
        dimensions.add(dimension2);
        modelDetail.setDimensions(dimensions);

        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("停留时长", "stay_hours", AggregateTypeEnum.SUM.name(), 1);
        measures.add(measure1);

        modelDetail.setMeasures(measures);
        modelDetail.setSqlQuery(
                "select imp_date,user_name as stay_hours_user_name,stay_hours,page from PUBLIC.s2_stay_time_statis");
        modelDetail.setQueryType("table_query");
        modelReq.setDomainId(1L);
        modelReq.setModelDetail(modelDetail);
        modelService.createModel(modelReq, user);
    }

    public void addModelRela_1() {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("user_name", "s2_pv_uv_statis_user_name", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(1L);
        modelRelaReq.setFromModelId(1L);
        modelRelaReq.setToModelId(2L);
        modelRelaReq.setJoinType("left join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, user);
    }

    public void addModelRela_2() {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("user_name", "stay_hours_user_name", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(1L);
        modelRelaReq.setFromModelId(1L);
        modelRelaReq.setToModelId(3L);
        modelRelaReq.setJoinType("left join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, user);
    }

    public void addDomain_2() {
        DomainReq domainReq = new DomainReq();
        domainReq.setName("艺人库");
        domainReq.setBizName("supersonic");
        domainReq.setParentId(0L);
        domainReq.setStatus(StatusEnum.ONLINE.getCode());
        domainReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        domainReq.setViewOrgs(Collections.singletonList("admin"));
        domainReq.setAdmins(Collections.singletonList("admin"));
        domainReq.setAdminOrgs(Collections.emptyList());
        domainService.createDomain(domainReq, user);
    }

    public void addModel_4() throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("艺人库");
        modelReq.setBizName("singer");
        modelReq.setDescription("艺人库");
        modelReq.setDatabaseId(1L);
        modelReq.setDomainId(2L);
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("admin"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Identify> identifiers = new ArrayList<>();
        Identify identify = new Identify("歌手名", IdentifyTypeEnum.primary.name(), "singer_name");
        identify.setEntityNames(Lists.newArrayList("歌手", "艺人"));
        identifiers.add(identify);
        modelDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionTypeEnum.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        dimensions.add(new Dim("活跃区域", "act_area",
                DimensionTypeEnum.categorical.name(), 1, 1));
        dimensions.add(new Dim("代表作", "song_name",
                DimensionTypeEnum.categorical.name(), 1));
        dimensions.add(new Dim("风格", "genre",
                DimensionTypeEnum.categorical.name(), 1, 1));
        modelDetail.setDimensions(dimensions);

        Measure measure1 = new Measure("播放量", "js_play_cnt", "sum", 1);
        Measure measure2 = new Measure("下载量", "down_cnt", "sum", 1);
        Measure measure3 = new Measure("收藏量", "favor_cnt", "sum", 1);
        modelDetail.setMeasures(Lists.newArrayList(measure1, measure2, measure3));
        modelDetail.setQueryType("table_query");
        modelDetail.setTableQuery("PUBLIC.singer");
        modelReq.setModelDetail(modelDetail);
        modelService.createModel(modelReq, user);
    }

    public void updateDimension() throws Exception {
        DimensionReq dimensionReq = new DimensionReq();
        dimensionReq.setModelId(1L);
        dimensionReq.setType(DimensionTypeEnum.categorical.name());
        dimensionReq.setId(4L);
        dimensionReq.setName("页面");
        dimensionReq.setBizName("page");
        dimensionReq.setModelId(3L);
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
        authGroupReq.setModelId(3L);
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
        authGroupReq.setModelId(3L);
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