package com.tencent.supersonic;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthGroup;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRule;
import com.tencent.supersonic.auth.api.authorization.service.AuthService;
import com.tencent.supersonic.chat.server.plugin.Plugin;
import com.tencent.supersonic.chat.server.plugin.PluginParseConfig;
import com.tencent.supersonic.chat.server.plugin.build.ParamOption;
import com.tencent.supersonic.chat.server.plugin.build.WebBase;
import com.tencent.supersonic.chat.server.service.PluginService;
import com.tencent.supersonic.common.pojo.JoinCondition;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.DataSetDetail;
import com.tencent.supersonic.headless.api.pojo.DataSetModelConfig;
import com.tencent.supersonic.headless.api.pojo.DefaultDisplayInfo;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.DimensionTimeTypeParams;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.Field;
import com.tencent.supersonic.headless.api.pojo.FieldParam;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.MeasureParam;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByFieldParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMeasureParams;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMetricParams;
import com.tencent.supersonic.headless.api.pojo.MetricParam;
import com.tencent.supersonic.headless.api.pojo.MetricTypeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.ModelDetail;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.pojo.TagTypeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.enums.DataType;
import com.tencent.supersonic.headless.api.pojo.enums.DimensionType;
import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.enums.SemanticType;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import com.tencent.supersonic.headless.api.pojo.request.DataSetReq;
import com.tencent.supersonic.headless.api.pojo.request.DatabaseReq;
import com.tencent.supersonic.headless.api.pojo.request.DimensionReq;
import com.tencent.supersonic.headless.api.pojo.request.DomainReq;
import com.tencent.supersonic.headless.api.pojo.request.MetricReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelReq;
import com.tencent.supersonic.headless.api.pojo.request.TagObjectReq;
import com.tencent.supersonic.headless.api.pojo.request.TagReq;
import com.tencent.supersonic.headless.api.pojo.request.TermReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.TagObjectResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.DatabaseService;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.DomainService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelRelaService;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.TagMetaService;
import com.tencent.supersonic.headless.server.service.TagObjectService;
import com.tencent.supersonic.headless.server.service.TermService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ModelDemoDataLoader {

    protected DatabaseResp tmpDatabaseResp = null;
    private User user = User.getFakeUser();
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
    @Autowired
    private DataSetService dataSetService;
    @Autowired
    private DataSourceProperties dataSourceProperties;
    @Autowired
    private TagObjectService tagObjectService;
    @Autowired
    private TagMetaService tagMetaService;
    @Autowired
    private TermService termService;
    @Autowired
    private PluginService pluginService;

    public void doRun() {
        try {
            DatabaseResp databaseResp = addDatabase();
            tmpDatabaseResp = databaseResp;
            DomainResp s2Domain = addDomain();
            TagObjectResp s2TagObject = addTagObjectUser(s2Domain);
            ModelResp userModel = addModel_1(s2Domain, databaseResp, s2TagObject);
            ModelResp pvUvModel = addModel_2(s2Domain, databaseResp);
            DimensionResp userDimension = getDimension("user_name", userModel);
            DimensionResp departmentDimension = getDimension("department", userModel);
            MetricResp metricUv = addMetric_uv(userModel, departmentDimension);
            MetricResp metricPv = getMetric("pv", pvUvModel);
            addMetric_pv_avg(metricPv, metricUv, departmentDimension, pvUvModel);
            ModelResp stayTimeModel = addModel_3(s2Domain, databaseResp);
            addModelRela_1(s2Domain, userModel, pvUvModel);
            addModelRela_2(s2Domain, userModel, stayTimeModel);
            DomainResp singerDomain = addDomain_2();
            TagObjectResp singerTagObject = addTagObjectSinger(singerDomain);
            ModelResp singerModel = addModel_4(singerDomain, databaseResp, singerTagObject);
            DimensionResp pageDimension = getDimension("page", stayTimeModel);
            updateDimension(stayTimeModel, pageDimension);
            updateMetric(stayTimeModel, departmentDimension, userDimension);
            addTags(userModel, singerModel);
            updateMetric_pv(pvUvModel, departmentDimension, userDimension, metricPv);
            DataSetResp s2DataSet = addDataSet_1(s2Domain);
            addDataSet_2(singerDomain, singerModel);
            addAuthGroup_1(stayTimeModel);
            addAuthGroup_2(stayTimeModel);
            addTerm(s2Domain);
            addTerm_1(s2Domain);
            addPlugin_1(s2DataSet, userDimension, userModel);
        } catch (Exception e) {
            log.error("Failed to add model demo data", e);
        }

    }

    private TagObjectResp addTagObjectUser(DomainResp s2Domain) throws Exception {
        TagObjectReq tagObjectReq = new TagObjectReq();
        tagObjectReq.setDomainId(s2Domain.getId());
        tagObjectReq.setName("用户");
        tagObjectReq.setBizName("user");
        User user = User.getFakeUser();
        return tagObjectService.create(tagObjectReq, user);
    }

    private TagObjectResp addTagObjectSinger(DomainResp singerDomain) throws Exception {
        TagObjectReq tagObjectReq = new TagObjectReq();
        tagObjectReq.setDomainId(singerDomain.getId());
        tagObjectReq.setName("艺人");
        tagObjectReq.setBizName("singer");
        User user = User.getFakeUser();
        return tagObjectService.create(tagObjectReq, user);
    }

    public DatabaseResp addDatabase() {
        String url = dataSourceProperties.getUrl();
        DatabaseReq databaseReq = new DatabaseReq();
        databaseReq.setName("数据实例");
        databaseReq.setDescription("样例数据库实例");
        if (StringUtils.isNotBlank(url)
                && url.toLowerCase().contains(DataType.MYSQL.getFeature().toLowerCase())) {
            databaseReq.setType(DataType.MYSQL.getFeature());
            databaseReq.setVersion("5.7");
        } else {
            databaseReq.setType(DataType.H2.getFeature());
        }
        databaseReq.setUrl(url);
        databaseReq.setUsername(dataSourceProperties.getUsername());
        databaseReq.setPassword(dataSourceProperties.getPassword());
        return databaseService.createOrUpdateDatabase(databaseReq, user);
    }

    public DomainResp addDomain() {
        DomainReq domainReq = new DomainReq();
        domainReq.setName("超音数");
        domainReq.setBizName("supersonic");
        domainReq.setParentId(0L);
        domainReq.setStatus(StatusEnum.ONLINE.getCode());
        domainReq.setViewers(Arrays.asList("admin", "tom"));
        domainReq.setViewOrgs(Collections.singletonList("1"));
        domainReq.setAdmins(Arrays.asList("admin", "jack"));
        domainReq.setAdminOrgs(Collections.emptyList());
        return domainService.createDomain(domainReq, user);
    }

    public ModelResp addModel_1(DomainResp s2Domain, DatabaseResp s2Database,
                                TagObjectResp s2TagObject) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("用户部门");
        modelReq.setBizName("user_department");
        modelReq.setDescription("用户部门信息");
        modelReq.setDatabaseId(s2Database.getId());
        modelReq.setDomainId(s2Domain.getId());
        modelReq.setTagObjectId(s2TagObject.getId());
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Arrays.asList("admin", "alice"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户", IdentifyType.primary.name(), "user_name", 1));
        modelDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        dimensions.add(new Dim("部门", "department",
                DimensionType.categorical.name(), 1));
        modelDetail.setDimensions(dimensions);
        List<Field> fields = Lists.newArrayList();
        fields.add(Field.builder().fieldName("user_name").dataType("Varchar").build());
        fields.add(Field.builder().fieldName("department").dataType("Varchar").build());
        modelDetail.setFields(fields);
        modelDetail.setMeasures(Collections.emptyList());
        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("select user_name,department from s2_user_department");
        modelReq.setModelDetail(modelDetail);
        return modelService.createModel(modelReq, user);
    }

    public ModelResp addModel_2(DomainResp s2Domain, DatabaseResp s2Database) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("PVUV统计");
        modelReq.setBizName("s2_pv_uv_statis");
        modelReq.setDescription("PVUV统计");
        modelReq.setDatabaseId(s2Database.getId());
        modelReq.setDomainId(s2Domain.getId());
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        List<Identify> identifiers = new ArrayList<>();
        ModelDetail modelDetail = new ModelDetail();
        identifiers.add(new Identify("用户名", IdentifyType.primary.name(), "user_name", 0));
        modelDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionType.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("", "page", DimensionType.categorical.name(), 0);
        dimension2.setExpr("page");
        dimensions.add(dimension2);
        modelDetail.setDimensions(dimensions);
        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("访问次数", "pv", AggOperatorEnum.SUM.name(), 1);
        measures.add(measure1);
        Measure measure2 = new Measure("访问用户数", "user_id", AggOperatorEnum.SUM.name(), 0);
        measures.add(measure2);
        modelDetail.setMeasures(measures);
        List<Field> fields = Lists.newArrayList();
        fields.add(Field.builder().fieldName("user_name").dataType("Varchar").build());
        fields.add(Field.builder().fieldName("imp_date").dataType("Date").build());
        fields.add(Field.builder().fieldName("page").dataType("Varchar").build());
        fields.add(Field.builder().fieldName("pv").dataType("Long").build());
        fields.add(Field.builder().fieldName("user_id").dataType("Varchar").build());
        modelDetail.setFields(fields);
        modelDetail.setSqlQuery("SELECT imp_date, user_name, page, 1 as pv, "
                + "user_name as user_id FROM s2_pv_uv_statis");
        modelDetail.setQueryType("sql_query");
        modelReq.setModelDetail(modelDetail);
        return modelService.createModel(modelReq, user);
    }

    public ModelResp addModel_3(DomainResp s2Domain, DatabaseResp s2Database) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("停留时长统计");
        modelReq.setBizName("s2_stay_time_statis");
        modelReq.setDescription("停留时长统计");
        modelReq.setDomainId(s2Domain.getId());
        modelReq.setDatabaseId(s2Database.getId());
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        List<Identify> identifiers = new ArrayList<>();
        ModelDetail modelDetail = new ModelDetail();
        identifiers.add(new Identify("用户", IdentifyType.primary.name(), "user_name", 0));
        modelDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionType.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dim dimension2 = new Dim("页面", "page", DimensionType.categorical.name(), 1);
        dimension2.setExpr("page");
        dimensions.add(dimension2);
        modelDetail.setDimensions(dimensions);

        List<Measure> measures = new ArrayList<>();
        Measure measure1 = new Measure("停留时长", "stay_hours", AggregateTypeEnum.SUM.name(), 1);
        measures.add(measure1);
        modelDetail.setMeasures(measures);
        List<Field> fields = Lists.newArrayList();
        fields.add(Field.builder().fieldName("user_name").dataType("Varchar").build());
        fields.add(Field.builder().fieldName("imp_date").dataType("Date").build());
        fields.add(Field.builder().fieldName("page").dataType("Varchar").build());
        fields.add(Field.builder().fieldName("stay_hours").dataType("Double").build());
        modelDetail.setFields(fields);
        modelDetail.setSqlQuery("select imp_date,user_name,stay_hours,page from s2_stay_time_statis");
        modelDetail.setQueryType("sql_query");
        modelReq.setModelDetail(modelDetail);
        return modelService.createModel(modelReq, user);
    }

    public void addModelRela_1(DomainResp s2Domain, ModelResp userDepartmentModel, ModelResp pvUvModel) {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("user_name", "user_name", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(s2Domain.getId());
        modelRelaReq.setFromModelId(userDepartmentModel.getId());
        modelRelaReq.setToModelId(pvUvModel.getId());
        modelRelaReq.setJoinType("left join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, user);
    }

    public void addModelRela_2(DomainResp s2Domain, ModelResp userDepartmentModel, ModelResp stayTimeModel) {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("user_name", "user_name", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(s2Domain.getId());
        modelRelaReq.setFromModelId(userDepartmentModel.getId());
        modelRelaReq.setToModelId(stayTimeModel.getId());
        modelRelaReq.setJoinType("left join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, user);
    }

    public DomainResp addDomain_2() {
        DomainReq domainReq = new DomainReq();
        domainReq.setName("艺人库");
        domainReq.setBizName("supersonic");
        domainReq.setParentId(0L);
        domainReq.setStatus(StatusEnum.ONLINE.getCode());
        domainReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        domainReq.setViewOrgs(Collections.singletonList("1"));
        domainReq.setAdmins(Arrays.asList("admin", "alice"));
        domainReq.setAdminOrgs(Collections.emptyList());
        return domainService.createDomain(domainReq, user);
    }

    public ModelResp addModel_4(DomainResp singerDomain,
                                DatabaseResp s2Database, TagObjectResp singerTagObject) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("艺人库");
        modelReq.setBizName("singer");
        modelReq.setDescription("艺人库");
        modelReq.setDatabaseId(s2Database.getId());
        modelReq.setDomainId(singerDomain.getId());
        modelReq.setTagObjectId(singerTagObject.getId());
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Identify> identifiers = new ArrayList<>();
        Identify identify = new Identify("歌手名", IdentifyType.primary.name(), "singer_name", 1);
        identify.setEntityNames(Lists.newArrayList("歌手", "艺人"));
        identifiers.add(identify);
        modelDetail.setIdentifiers(identifiers);

        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionType.time.name(), 0);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        dimensions.add(new Dim("活跃区域", "act_area",
                DimensionType.categorical.name(), 1, 1));
        dimensions.add(new Dim("代表作", "song_name",
                DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("风格", "genre",
                DimensionType.categorical.name(), 1, 1));
        modelDetail.setDimensions(dimensions);

        Measure measure1 = new Measure("播放量", "js_play_cnt", "sum", 1);
        Measure measure2 = new Measure("下载量", "down_cnt", "sum", 1);
        Measure measure3 = new Measure("收藏量", "favor_cnt", "sum", 1);
        modelDetail.setMeasures(Lists.newArrayList(measure1, measure2, measure3));
        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("select imp_date, singer_name, act_area, song_name, genre, "
                + "js_play_cnt, down_cnt, favor_cnt from singer");
        modelReq.setModelDetail(modelDetail);
        return modelService.createModel(modelReq, user);
    }

    private void addTags(ModelResp userModel, ModelResp singerModel) {
        addTag(dimensionService.getDimension("department", userModel.getId()).getId(),
                TagDefineType.DIMENSION);
        addTag(dimensionService.getDimension("act_area", singerModel.getId()).getId(),
                TagDefineType.DIMENSION);
        addTag(dimensionService.getDimension("song_name", singerModel.getId()).getId(),
                TagDefineType.DIMENSION);
        addTag(dimensionService.getDimension("genre", singerModel.getId()).getId(),
                TagDefineType.DIMENSION);
        addTag(dimensionService.getDimension("singer_name", singerModel.getId()).getId(),
                TagDefineType.DIMENSION);
        addTag(metricService.getMetric(singerModel.getId(), "js_play_cnt").getId(),
                TagDefineType.METRIC);
    }

    private void addTag(Long itemId, TagDefineType tagDefineType) {
        TagReq tagReq = new TagReq();
        tagReq.setTagDefineType(tagDefineType);
        tagReq.setItemId(itemId);
        tagMetaService.create(tagReq, User.getFakeUser());
    }

    public void updateDimension(ModelResp stayTimeModel, DimensionResp pageDimension) throws Exception {
        DimensionReq dimensionReq = new DimensionReq();
        dimensionReq.setType(DimensionType.categorical.name());
        dimensionReq.setId(pageDimension.getId());
        dimensionReq.setName("页面");
        dimensionReq.setBizName("page");
        dimensionReq.setModelId(stayTimeModel.getId());
        dimensionReq.setAlias("page");
        dimensionReq.setSemanticType(SemanticType.CATEGORY.name());
        dimensionReq.setSensitiveLevel(SensitiveLevelEnum.HIGH.getCode());
        dimensionReq.setDescription("页面");
        dimensionReq.setExpr("page");
        dimensionReq.setDimValueMaps(Collections.emptyList());
        dimensionService.updateDimension(dimensionReq, user);
    }

    public void updateMetric(ModelResp stayTimeModel, DimensionResp departmentDimension,
                             DimensionResp userDimension) throws Exception {
        MetricResp stayHoursMetric =
                metricService.getMetric(stayTimeModel.getId(), "stay_hours");
        MetricReq metricReq = new MetricReq();
        metricReq.setModelId(stayTimeModel.getId());
        metricReq.setId(stayHoursMetric.getId());
        metricReq.setName("停留时长");
        metricReq.setBizName("stay_hours");
        metricReq.setSensitiveLevel(SensitiveLevelEnum.HIGH.getCode());
        metricReq.setDescription("停留时长");
        metricReq.setClassifications(Collections.singletonList("核心指标"));
        metricReq.setAlias("访问时长");
        MetricDefineByMeasureParams metricTypeParams = new MetricDefineByMeasureParams();
        metricTypeParams.setExpr("s2_stay_time_statis_stay_hours");
        List<MeasureParam> measures = new ArrayList<>();
        MeasureParam measure = new MeasureParam("s2_stay_time_statis_stay_hours",
                "", AggOperatorEnum.SUM.getOperator());
        measures.add(measure);
        metricTypeParams.setMeasures(measures);
        metricReq.setMetricDefineByMeasureParams(metricTypeParams);
        metricReq.setMetricDefineType(MetricDefineType.MEASURE);
        metricReq.setRelateDimension(getRelateDimension(
                Lists.newArrayList(departmentDimension.getId(), userDimension.getId())));
        metricService.updateMetric(metricReq, user);
    }

    public void updateMetric_pv(ModelResp pvUvModel, DimensionResp departmentDimension,
                                DimensionResp userDimension, MetricResp metricPv) throws Exception {
        MetricReq metricReq = new MetricReq();
        metricReq.setModelId(pvUvModel.getId());
        metricReq.setId(metricPv.getId());
        metricReq.setName("访问次数");
        metricReq.setBizName("pv");
        metricReq.setDescription("一段时间内用户的访问次数");
        MetricDefineByMeasureParams metricTypeParams = new MetricDefineByMeasureParams();
        metricTypeParams.setExpr("s2_pv_uv_statis_pv");
        List<MeasureParam> measures = new ArrayList<>();
        MeasureParam measure = new MeasureParam("s2_pv_uv_statis_pv",
                "", AggOperatorEnum.SUM.getOperator());
        measures.add(measure);
        metricTypeParams.setMeasures(measures);
        metricReq.setMetricDefineByMeasureParams(metricTypeParams);
        metricReq.setMetricDefineType(MetricDefineType.MEASURE);
        metricReq.setRelateDimension(getRelateDimension(
                Lists.newArrayList(departmentDimension.getId(), userDimension.getId())));
        metricService.updateMetric(metricReq, user);
    }

    public MetricResp addMetric_uv(ModelResp uvModel, DimensionResp departmentDimension) throws Exception {
        MetricReq metricReq = new MetricReq();
        metricReq.setModelId(uvModel.getId());
        metricReq.setName("访问用户数");
        metricReq.setBizName("uv");
        metricReq.setSensitiveLevel(SensitiveLevelEnum.LOW.getCode());
        metricReq.setDescription("访问的用户个数");
        metricReq.setAlias("UV,访问人数");
        MetricDefineByFieldParams metricTypeParams = new MetricDefineByFieldParams();
        metricTypeParams.setExpr("count(distinct user_id)");
        List<FieldParam> fieldParams = new ArrayList<>();
        fieldParams.add(new FieldParam("user_id"));
        metricTypeParams.setFields(fieldParams);
        metricReq.setMetricDefineByFieldParams(metricTypeParams);
        metricReq.setMetricDefineType(MetricDefineType.FIELD);
        metricReq.setRelateDimension(getRelateDimension(
                Lists.newArrayList(departmentDimension.getId())));
        return metricService.createMetric(metricReq, user);
    }

    public MetricResp addMetric_pv_avg(MetricResp metricPv, MetricResp metricUv,
                                       DimensionResp departmentDimension, ModelResp pvModel) throws Exception {
        MetricReq metricReq = new MetricReq();
        metricReq.setModelId(pvModel.getId());
        metricReq.setName("人均访问次数");
        metricReq.setBizName("pv_avg");
        metricReq.setSensitiveLevel(SensitiveLevelEnum.HIGH.getCode());
        metricReq.setDescription("每个用户平均访问的次数");
        metricReq.setClassifications(Collections.singletonList("核心指标"));
        metricReq.setAlias("平均访问次数");
        MetricDefineByMetricParams metricTypeParams = new MetricDefineByMetricParams();
        metricTypeParams.setExpr("pv/uv");
        List<MetricParam> metrics = new ArrayList<>();
        MetricParam pv = new MetricParam(metricPv.getId(), metricPv.getBizName());
        MetricParam uv = new MetricParam(metricUv.getId(), metricUv.getBizName());
        metrics.add(pv);
        metrics.add(uv);
        metricTypeParams.setMetrics(metrics);
        metricReq.setMetricDefineByMetricParams(metricTypeParams);
        metricReq.setMetricDefineType(MetricDefineType.METRIC);
        metricReq.setRelateDimension(getRelateDimension(Lists.newArrayList(departmentDimension.getId())));
        return metricService.createMetric(metricReq, user);
    }

    public DataSetResp addDataSet_1(DomainResp s2Domain) {
        DataSetReq dataSetReq = new DataSetReq();
        dataSetReq.setName("超音数");
        dataSetReq.setBizName("s2");
        dataSetReq.setDomainId(s2Domain.getId());
        dataSetReq.setDescription("包含超音数访问统计相关的指标和维度等");
        dataSetReq.setAdmins(Lists.newArrayList("admin"));
        List<DataSetModelConfig> dataSetModelConfigs = getDataSetModelConfigs(s2Domain.getId());
        DataSetDetail dataSetDetail = new DataSetDetail();
        dataSetDetail.setDataSetModelConfigs(dataSetModelConfigs);
        dataSetReq.setDataSetDetail(dataSetDetail);
        dataSetReq.setTypeEnum(TypeEnums.DATASET);
        QueryConfig queryConfig = new QueryConfig();
        MetricTypeDefaultConfig metricTypeDefaultConfig = new MetricTypeDefaultConfig();
        TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();
        timeDefaultConfig.setTimeMode(TimeMode.RECENT);
        timeDefaultConfig.setUnit(7);
        metricTypeDefaultConfig.setTimeDefaultConfig(timeDefaultConfig);
        queryConfig.setMetricTypeDefaultConfig(metricTypeDefaultConfig);
        dataSetReq.setQueryConfig(queryConfig);
        return dataSetService.save(dataSetReq, User.getFakeUser());
    }

    public void addDataSet_2(DomainResp singerDomain, ModelResp singerModel) {
        DataSetReq dataSetReq = new DataSetReq();
        dataSetReq.setName("艺人库");
        dataSetReq.setBizName("singer");
        dataSetReq.setDomainId(singerDomain.getId());
        dataSetReq.setDescription("包含艺人相关标签和指标信息");
        dataSetReq.setAdmins(Lists.newArrayList("admin", "jack"));
        List<DataSetModelConfig> dataSetModelConfigs = getDataSetModelConfigs(singerDomain.getId());
        DataSetDetail dataSetDetail = new DataSetDetail();
        dataSetDetail.setDataSetModelConfigs(dataSetModelConfigs);
        dataSetReq.setDataSetDetail(dataSetDetail);
        dataSetReq.setTypeEnum(TypeEnums.DATASET);
        QueryConfig queryConfig = new QueryConfig();
        TagTypeDefaultConfig tagTypeDefaultConfig = new TagTypeDefaultConfig();
        TimeDefaultConfig tagTimeDefaultConfig = new TimeDefaultConfig();
        tagTimeDefaultConfig.setTimeMode(TimeMode.LAST);
        tagTimeDefaultConfig.setUnit(7);
        tagTypeDefaultConfig.setTimeDefaultConfig(tagTimeDefaultConfig);
        DefaultDisplayInfo defaultDisplayInfo = new DefaultDisplayInfo();
        defaultDisplayInfo.setDimensionIds(dataSetModelConfigs.get(0).getDimensions());
        MetricResp jsPlayCntMetric = getMetric("js_play_cnt", singerModel);
        defaultDisplayInfo.setMetricIds(Lists.newArrayList(jsPlayCntMetric.getId()));
        tagTypeDefaultConfig.setDefaultDisplayInfo(defaultDisplayInfo);
        MetricTypeDefaultConfig metricTypeDefaultConfig = new MetricTypeDefaultConfig();
        TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();
        timeDefaultConfig.setTimeMode(TimeMode.RECENT);
        timeDefaultConfig.setUnit(7);
        metricTypeDefaultConfig.setTimeDefaultConfig(timeDefaultConfig);
        queryConfig.setTagTypeDefaultConfig(tagTypeDefaultConfig);
        queryConfig.setMetricTypeDefaultConfig(metricTypeDefaultConfig);
        dataSetReq.setQueryConfig(queryConfig);
        dataSetService.save(dataSetReq, User.getFakeUser());
    }

    public void addTerm(DomainResp s2Domain) {
        TermReq termReq = new TermReq();
        termReq.setName("近期");
        termReq.setDescription("指近10天");
        termReq.setAlias(Lists.newArrayList("近一段时间"));
        termReq.setDomainId(s2Domain.getId());
        termService.saveOrUpdate(termReq, User.getFakeUser());
    }

    public void addTerm_1(DomainResp s2Domain) {
        TermReq termReq = new TermReq();
        termReq.setName("核心用户");
        termReq.setDescription("核心用户指tom和lucy");
        termReq.setAlias(Lists.newArrayList("VIP用户"));
        termReq.setDomainId(s2Domain.getId());
        termService.saveOrUpdate(termReq, User.getFakeUser());
    }

    public void addAuthGroup_1(ModelResp stayTimeModel) {
        AuthGroup authGroupReq = new AuthGroup();
        authGroupReq.setModelId(stayTimeModel.getId());
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

    public void addAuthGroup_2(ModelResp stayTimeModel) {
        AuthGroup authGroupReq = new AuthGroup();
        authGroupReq.setModelId(stayTimeModel.getId());
        authGroupReq.setName("tom_row_permission");

        List<AuthRule> authRules = new ArrayList<>();
        authGroupReq.setAuthRules(authRules);
        authGroupReq.setDimensionFilters(Collections.singletonList("user_name = 'tom'"));
        authGroupReq.setDimensionFilterDescription("用户名='tom'");
        authGroupReq.setAuthorizedUsers(Collections.singletonList("tom"));
        authGroupReq.setAuthorizedDepartmentIds(Collections.emptyList());
        authService.addOrUpdateAuthGroup(authGroupReq);
    }

    private void addPlugin_1(DataSetResp s2DataSet, DimensionResp userDimension,
                             ModelResp userModel) {
        Plugin plugin1 = new Plugin();
        plugin1.setType("WEB_PAGE");
        plugin1.setDataSetList(Arrays.asList(s2DataSet.getId()));
        plugin1.setPattern("用于分析超音数的流量概况，包含UV、PV等核心指标的追踪。P.S. 仅作为示例展示，无实际看板");
        plugin1.setName("超音数流量分析看板");
        PluginParseConfig pluginParseConfig = new PluginParseConfig();
        pluginParseConfig.setDescription(plugin1.getPattern());
        pluginParseConfig.setName(plugin1.getName());
        pluginParseConfig.setExamples(Lists.newArrayList("tom最近访问超音数情况怎么样"));
        plugin1.setParseModeConfig(JSONObject.toJSONString(pluginParseConfig));
        WebBase webBase = new WebBase();
        webBase.setUrl("www.yourbi.com");
        ParamOption paramOption = new ParamOption();
        paramOption.setKey("name");
        paramOption.setParamType(ParamOption.ParamType.SEMANTIC);
        paramOption.setElementId(userDimension.getId());
        paramOption.setModelId(userModel.getId());
        List<ParamOption> paramOptions = Arrays.asList(paramOption);
        webBase.setParamOptions(paramOptions);
        plugin1.setConfig(JsonUtil.toString(webBase));
        pluginService.createPlugin(plugin1, user);
    }

    private RelateDimension getRelateDimension(List<Long> dimensionIds) {
        RelateDimension relateDimension = new RelateDimension();
        for (Long id : dimensionIds) {
            relateDimension.getDrillDownDimensions().add(new DrillDownDimension(id));
        }
        return relateDimension;
    }

    private DimensionResp getDimension(String bizName, ModelResp model) {
        return dimensionService.getDimension(bizName, model.getId());
    }

    private MetricResp getMetric(String bizName, ModelResp model) {
        return metricService.getMetric(model.getId(), bizName);
    }

    protected List<DataSetModelConfig> getDataSetModelConfigs(Long domainId) {
        List<DataSetModelConfig> dataSetModelConfigs = Lists.newArrayList();
        List<ModelResp> modelByDomainIds =
                modelService.getModelByDomainIds(Lists.newArrayList(domainId));

        for (ModelResp modelResp : modelByDomainIds) {
            DataSetModelConfig dataSetModelConfig = new DataSetModelConfig();
            dataSetModelConfig.setId(modelResp.getId());
            MetaFilter metaFilter = new MetaFilter();
            metaFilter.setModelIds(Lists.newArrayList(modelResp.getId()));
            List<Long> metrics = metricService.getMetrics(metaFilter)
                    .stream().map(MetricResp::getId).collect(Collectors.toList());
            dataSetModelConfig.setMetrics(metrics);
            List<Long> dimensions = dimensionService.getDimensions(metaFilter)
                    .stream().map(DimensionResp::getId).collect(Collectors.toList());
            dataSetModelConfig.setMetrics(metrics);
            dataSetModelConfig.setDimensions(dimensions);
            dataSetModelConfigs.add(dataSetModelConfig);
        }
        return dataSetModelConfigs;
    }

}
