package com.tencent.supersonic.demo;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthGroup;
import com.tencent.supersonic.auth.api.authorization.pojo.AuthRule;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.DatasetTool;
import com.tencent.supersonic.chat.server.agent.ToolConfig;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.PluginParseConfig;
import com.tencent.supersonic.chat.server.plugin.build.WebBase;
import com.tencent.supersonic.chat.server.plugin.build.webservice.WebServiceQuery;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.JoinCondition;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.*;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.api.pojo.enums.DimensionType;
import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.enums.SemanticType;
import com.tencent.supersonic.headless.api.pojo.request.*;
import com.tencent.supersonic.headless.api.pojo.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
@Order(1)
public class S2VisitsDemo extends S2BaseDemo {

    public static final String AGENT_NAME = "超音数分析助手";

    public void doRun() {
        try {
            // create domain
            DomainResp s2Domain = addDomain();

            // create models
            ModelResp userModel = addModel_1(s2Domain, demoDatabase);
            ModelResp pvUvModel = addModel_2(s2Domain, demoDatabase);
            ModelResp stayTimeModel = addModel_3(s2Domain, demoDatabase);
            addModelRela(s2Domain, pvUvModel, userModel, "user_name");
            addModelRela(s2Domain, stayTimeModel, userModel, "user_name");

            // create metrics and dimensions
            DimensionResp departmentDimension = getDimension("department", userModel);
            MetricResp metricUv = addMetric_uv(pvUvModel, departmentDimension);

            DimensionResp pageDimension = getDimension("visits_page", stayTimeModel);
            updateDimension(stayTimeModel, pageDimension);
            DimensionResp userDimension = getDimension("user_name", userModel);
            MetricResp metricPv = addMetric_pv(pvUvModel, departmentDimension, userDimension);

            addMetric_pv_avg(metricPv, metricUv, departmentDimension, pvUvModel);

            // create dict conf for dimensions
            enableDimensionValue(departmentDimension);
            enableDimensionValue(userDimension);

            // create data set
            DataSetResp s2DataSet = addDataSet(s2Domain);
            addAuthGroup_1(stayTimeModel);
            addAuthGroup_2(stayTimeModel);

            // create terms and plugin
            addTerm(s2Domain);
            addPlugin();

            // load dict word
            loadDictWord();

            // create agent
            Integer agentId = addAgent(s2DataSet.getId());
            addSampleChats(agentId);
            updateQueryScore(1L);
            updateQueryScore(4L);
        } catch (Exception e) {
            log.error("Failed to add S2Visits demo data", e);
        }
    }

    @Override
    public boolean checkNeedToRun() {
        List<DomainResp> domainList = domainService.getDomainList();
        for (DomainResp domainResp : domainList) {
            if (domainResp.getBizName().equalsIgnoreCase("supersonic")) {
                log.info("Already exist domain:supersonic, no need to run demo");
                return false;
            }
        }
        return true;
    }

    private void addSampleChats(Integer agentId) {
        Long chatId = chatManageService.addChat(defaultUser, "样例对话1", agentId);
        submitText(chatId.intValue(), agentId, "访问过超音数的部门有哪些");
        submitText(chatId.intValue(), agentId, "按部门统计近7天访问次数");
        submitText(chatId.intValue(), agentId, "alice 停留时长");
    }

    private Integer addAgent(long dataSetId) {
        Agent agent = new Agent();
        agent.setName(AGENT_NAME);
        agent.setDescription("帮忙您对超音数产品的用户访问情况做分析");
        agent.setStatus(1);
        agent.setEnableSearch(1);
        agent.setExamples(Lists.newArrayList("近15天超音数访问次数汇总", "按部门统计超音数的访问人数", "对比alice和lucy的停留时长",
                "过去30天访问次数最高的部门top3", "近1个月总访问次数超过100次的部门有几个", "过去半个月每个核心用户的总停留时长",
                "今年以来访问次数最高的一天是哪一天"));

        // configure tools
        ToolConfig toolConfig = new ToolConfig();
        DatasetTool datasetTool = new DatasetTool();
        datasetTool.setId("1");
        datasetTool.setType(AgentToolType.DATASET);
        datasetTool.setDataSetIds(Lists.newArrayList(dataSetId));
        toolConfig.getTools().add(datasetTool);
        agent.setToolConfig(JSONObject.toJSONString(toolConfig));

        // configure chat apps
        Map<String, ChatApp> chatAppConfig =
                Maps.newHashMap(ChatAppManager.getAllApps(AppModule.CHAT));
        chatAppConfig.values().forEach(app -> app.setChatModelId(demoChatModel.getId()));
        agent.setChatAppConfig(chatAppConfig);
        agent.setAdmins(Lists.newArrayList("tom"));
        agent.setViewers(Lists.newArrayList("alice", "jack"));
        Agent agentCreated = agentService.createAgent(agent, defaultUser);
        return agentCreated.getId();
    }

    private DomainResp addDomain() {
        DomainReq domainReq = new DomainReq();
        domainReq.setName("产品数据域");
        domainReq.setBizName("supersonic");
        domainReq.setParentId(0L);
        domainReq.setStatus(StatusEnum.ONLINE.getCode());
        domainReq.setViewers(Arrays.asList("admin", "tom"));
        domainReq.setAdmins(Arrays.asList("admin", "jack"));
        domainReq.setIsOpen(1);
        return domainService.createDomain(domainReq, defaultUser);
    }

    private ModelResp addModel_1(DomainResp s2Domain, DatabaseResp s2Database) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("用户部门");
        modelReq.setBizName("user_department");
        modelReq.setDescription("用户部门信息");
        modelReq.setDatabaseId(s2Database.getId());
        modelReq.setDomainId(s2Domain.getId());
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Arrays.asList("admin", "alice"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("用户名", IdentifyType.primary.name(), "user_name", 1));
        modelDetail.setIdentifiers(identifiers);

        List<Dimension> dimensions = new ArrayList<>();
        dimensions.add(new Dimension("部门", "department", DimensionType.categorical, 1));
        modelDetail.setDimensions(dimensions);
        List<Field> fields = Lists.newArrayList();
        fields.add(Field.builder().fieldName("user_name").dataType("Varchar").build());
        fields.add(Field.builder().fieldName("department").dataType("Varchar").build());
        modelDetail.setFields(fields);
        modelDetail.setMeasures(Collections.emptyList());
        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("select * from s2_user_department");
        modelReq.setModelDetail(modelDetail);
        return modelService.createModel(modelReq, defaultUser);
    }

    private ModelResp addModel_2(DomainResp s2Domain, DatabaseResp s2Database) throws Exception {
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
        identifiers.add(new Identify("用户名", IdentifyType.foreign.name(), "user_name", 1));
        modelDetail.setIdentifiers(identifiers);

        List<Dimension> dimensions = new ArrayList<>();
        Dimension dimension1 = new Dimension("数据日期", "imp_date", DimensionType.partition_time, 1);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dimension dimension2 = new Dimension("", "page", DimensionType.categorical, 1);
        dimension2.setExpr("page");
        dimensions.add(dimension2);
        modelDetail.setDimensions(dimensions);
        List<Field> fields = Lists.newArrayList();
        fields.add(Field.builder().fieldName("user_name").dataType("Varchar").build());
        fields.add(Field.builder().fieldName("imp_date").dataType("Date").build());
        fields.add(Field.builder().fieldName("page").dataType("Varchar").build());
        modelDetail.setFields(fields);
        modelDetail.setSqlQuery("SELECT * FROM s2_pv_uv_statis");
        modelDetail.setQueryType("sql_query");
        modelReq.setModelDetail(modelDetail);
        return modelService.createModel(modelReq, defaultUser);
    }

    private ModelResp addModel_3(DomainResp s2Domain, DatabaseResp s2Database) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("停留时长统计");
        modelReq.setBizName("s2_stay_time_statis");
        modelReq.setDescription("停留时长统计");
        modelReq.setDomainId(s2Domain.getId());
        modelReq.setDatabaseId(s2Database.getId());
        modelReq.setViewers(Arrays.asList("admin", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        List<Identify> identifiers = new ArrayList<>();
        ModelDetail modelDetail = new ModelDetail();
        identifiers.add(new Identify("用户名", IdentifyType.foreign.name(), "user_name", 1));
        modelDetail.setIdentifiers(identifiers);

        List<Dimension> dimensions = new ArrayList<>();
        Dimension dimension1 = new Dimension("数据日期", "imp_date", DimensionType.partition_time, 1);
        dimension1.setTypeParams(new DimensionTimeTypeParams());
        dimensions.add(dimension1);
        Dimension dimension2 = new Dimension("页面", "visits_page", DimensionType.categorical, 1);
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
        modelDetail.setSqlQuery("select * from s2_stay_time_statis");
        modelDetail.setQueryType("sql_query");
        modelReq.setModelDetail(modelDetail);
        return modelService.createModel(modelReq, defaultUser);
    }

    private void addModelRela(DomainResp s2Domain, ModelResp fromModel, ModelResp toModel,
            String joinField) {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition(joinField, joinField, FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(s2Domain.getId());
        modelRelaReq.setFromModelId(fromModel.getId());
        modelRelaReq.setToModelId(toModel.getId());
        modelRelaReq.setJoinType("left join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, defaultUser);
    }

    private void updateDimension(ModelResp stayTimeModel, DimensionResp pageDimension)
            throws Exception {
        DimensionReq dimensionReq = new DimensionReq();
        dimensionReq.setType(DimensionType.categorical.name());
        dimensionReq.setId(pageDimension.getId());
        dimensionReq.setName("页面");
        dimensionReq.setBizName("page");
        dimensionReq.setModelId(stayTimeModel.getId());
        dimensionReq.setSemanticType(SemanticType.CATEGORY.name());
        dimensionReq.setSensitiveLevel(SensitiveLevelEnum.MID.getCode());
        dimensionReq.setDescription("页面");
        dimensionReq.setExpr("page");
        dimensionReq.setDimValueMaps(Collections.emptyList());
        dimensionService.updateDimension(dimensionReq, defaultUser);
    }

    private MetricResp addMetric_pv(ModelResp pvUvModel, DimensionResp departmentDimension,
            DimensionResp userDimension) throws Exception {
        MetricReq metricReq = new MetricReq();
        metricReq.setModelId(pvUvModel.getId());
        metricReq.setName("访问次数");
        metricReq.setBizName("pv");
        metricReq.setDescription("一段时间内用户的访问次数");
        MetricDefineByFieldParams metricTypeParams = new MetricDefineByFieldParams();
        metricTypeParams.setExpr("count(1)");
        // List<FieldParam> fieldParams = new ArrayList<>();
        // fieldParams.add(new FieldParam("imp_date"));
        // metricTypeParams.setFields(fieldParams);
        metricReq.setMetricDefineByFieldParams(metricTypeParams);
        metricReq.setMetricDefineType(MetricDefineType.FIELD);
        metricReq.setRelateDimension(getRelateDimension(
                Lists.newArrayList(departmentDimension.getId(), userDimension.getId())));
        return metricService.createMetric(metricReq, defaultUser);
    }

    private MetricResp addMetric_uv(ModelResp uvModel, DimensionResp departmentDimension)
            throws Exception {
        MetricReq metricReq = new MetricReq();
        metricReq.setModelId(uvModel.getId());
        metricReq.setName("访问用户数");
        metricReq.setBizName("uv");
        metricReq.setSensitiveLevel(SensitiveLevelEnum.LOW.getCode());
        metricReq.setDescription("访问的用户个数");
        metricReq.setAlias("UV,访问人数");
        MetricDefineByFieldParams metricTypeParams = new MetricDefineByFieldParams();
        metricTypeParams.setExpr("count(distinct user_name)");
        List<FieldParam> fieldParams = new ArrayList<>();
        fieldParams.add(new FieldParam("user_name"));
        metricTypeParams.setFields(fieldParams);
        metricReq.setMetricDefineByFieldParams(metricTypeParams);
        metricReq.setMetricDefineType(MetricDefineType.FIELD);
        metricReq.setRelateDimension(
                getRelateDimension(Lists.newArrayList(departmentDimension.getId())));
        return metricService.createMetric(metricReq, defaultUser);
    }

    private MetricResp addMetric_pv_avg(MetricResp metricPv, MetricResp metricUv,
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
        metricReq.setRelateDimension(
                getRelateDimension(Lists.newArrayList(departmentDimension.getId())));
        return metricService.createMetric(metricReq, defaultUser);
    }

    private DataSetResp addDataSet(DomainResp s2Domain) {
        DataSetReq dataSetReq = new DataSetReq();
        dataSetReq.setName("超音数数据集");
        dataSetReq.setBizName("s2");
        dataSetReq.setDomainId(s2Domain.getId());
        dataSetReq.setDescription("包含超音数访问统计相关的指标和维度等");
        dataSetReq.setAdmins(Lists.newArrayList("admin"));
        List<DataSetModelConfig> dataSetModelConfigs = getDataSetModelConfigs(s2Domain.getId());
        DataSetDetail dataSetDetail = new DataSetDetail();
        dataSetDetail.setDataSetModelConfigs(dataSetModelConfigs);
        dataSetReq.setDataSetDetail(dataSetDetail);
        dataSetReq.setTypeEnum(TypeEnums.DATASET);
        return dataSetService.save(dataSetReq, defaultUser);
    }

    private void addTerm(DomainResp s2Domain) {
        TermReq termReq = new TermReq();
        termReq.setName("近期");
        termReq.setDescription("指近10天");
        termReq.setAlias(Lists.newArrayList("近一段时间"));
        termReq.setDomainId(s2Domain.getId());
        termService.saveOrUpdate(termReq, defaultUser);

        TermReq termReq1 = new TermReq();
        termReq1.setName("核心用户");
        termReq1.setDescription("用户为tom和lucy");
        termReq1.setAlias(Lists.newArrayList("VIP用户"));
        termReq1.setDomainId(s2Domain.getId());
        termService.saveOrUpdate(termReq1, defaultUser);
    }

    private void addAuthGroup_1(ModelResp stayTimeModel) {
        AuthGroup authGroupReq = new AuthGroup();
        authGroupReq.setModelId(stayTimeModel.getId());
        authGroupReq.setName("jack_column_permission");

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

    private void addAuthGroup_2(ModelResp model) {
        AuthGroup authGroupReq = new AuthGroup();
        authGroupReq.setModelId(model.getId());
        authGroupReq.setName("tom_row_permission");

        List<AuthRule> authRules = new ArrayList<>();
        authGroupReq.setAuthRules(authRules);
        authGroupReq.setDimensionFilters(Collections.singletonList("user_name = 'tom'"));
        authGroupReq.setAuthorizedUsers(Collections.singletonList("tom"));
        authGroupReq.setAuthorizedDepartmentIds(Collections.emptyList());
        authService.addOrUpdateAuthGroup(authGroupReq);
    }

    private void addPlugin() {
        ChatPlugin plugin1 = new ChatPlugin();
        plugin1.setType(WebServiceQuery.QUERY_MODE);
        plugin1.setDataSetList(Collections.singletonList(-1L));
        plugin1.setPattern("用于分析超音数的流量概况，包含UV、PV等核心指标的追踪。P.S. 仅作为示例展示，无实际内容");
        plugin1.setName("超音数流量分析小助手");
        PluginParseConfig pluginParseConfig = new PluginParseConfig();
        pluginParseConfig.setDescription(plugin1.getPattern());
        pluginParseConfig.setName(plugin1.getName());
        pluginParseConfig.setExamples(Lists.newArrayList("tom最近访问超音数情况怎么样"));
        plugin1.setParseModeConfig(JSONObject.toJSONString(pluginParseConfig));
        WebBase webBase = new WebBase();
        webBase.setUrl("http://localhost:9080/api/chat/plugin/pluginDemo");
        webBase.setParamOptions(Lists.newArrayList());
        plugin1.setConfig(JsonUtil.toString(webBase));
        pluginService.createPlugin(plugin1, defaultUser);
    }

}
