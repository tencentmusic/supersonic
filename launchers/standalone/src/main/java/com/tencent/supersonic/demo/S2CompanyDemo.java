package com.tencent.supersonic.demo;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.DatasetTool;
import com.tencent.supersonic.chat.server.agent.ToolConfig;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.JoinCondition;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.*;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.api.pojo.enums.DimensionType;
import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import com.tencent.supersonic.headless.api.pojo.request.DataSetReq;
import com.tencent.supersonic.headless.api.pojo.request.DomainReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
@Order(2)
public class S2CompanyDemo extends S2BaseDemo {

    public static final String AGENT_NAME = "企业分析助手";

    public void doRun() {
        try {
            DomainResp domain = addDomain();

            ModelResp model_company = addModel_1(domain, demoDatabase);
            ModelResp model_brand = addModel_2(domain, demoDatabase);
            ModelResp model_brand_revenue = addModel_3(domain, demoDatabase);

            addModelRela(domain, model_brand, model_company, "company_id");
            addModelRela(domain, model_brand_revenue, model_brand, "brand_id");

            DataSetResp dataset = addDataSet(domain);
            Long dataSetId = dataset.getId();
            addAgent(dataSetId);
        } catch (Exception e) {
            log.error("Failed to add bench mark demo data", e);
        }
    }

    @Override
    protected boolean checkNeedToRun() {
        List<DomainResp> domainList = domainService.getDomainList();
        for (DomainResp domainResp : domainList) {
            if (domainResp.getBizName().equalsIgnoreCase("corporate")) {
                log.info("Already exist domain:corporate, no need to run demo");
                return false;
            }
        }
        return true;
    }

    public DomainResp addDomain() {
        DomainReq domainReq = new DomainReq();
        domainReq.setName("企业数据域");
        domainReq.setBizName("corporate");
        domainReq.setParentId(0L);
        domainReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        domainReq.setViewOrgs(Collections.singletonList("1"));
        domainReq.setAdmins(Collections.singletonList("admin"));
        domainReq.setAdminOrgs(Collections.emptyList());
        return domainService.createDomain(domainReq, defaultUser);
    }

    public ModelResp addModel_1(DomainResp domain, DatabaseResp database) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("公司维度");
        modelReq.setBizName("company");
        modelReq.setDatabaseId(database.getId());
        modelReq.setDomainId(domain.getId());
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Dimension> dimensions = new ArrayList<>();
        modelDetail.setDimensions(dimensions);

        dimensions.add(new Dimension("公司名称", "company_name", DimensionType.categorical, 1));
        dimensions.add(new Dimension("总部地点", "headquarter_address", DimensionType.categorical, 1));
        dimensions.add(new Dimension("成立时间", "company_established_time", DimensionType.time, 1));
        dimensions.add(new Dimension("创始人", "founder", DimensionType.categorical, 1));
        dimensions.add(new Dimension("首席执行官", "ceo", DimensionType.categorical, 1));

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("公司id", IdentifyType.primary.name(), "company_id"));
        modelDetail.setIdentifiers(identifiers);

        List<Measure> measures = new ArrayList<>();
        measures.add(new Measure("年营业额", "annual_turnover", AggOperatorEnum.SUM.name(), 1));
        Measure measure = new Measure("员工数", "employee_count", AggOperatorEnum.SUM.name(), 1);
        measures.add(measure);
        modelDetail.setMeasures(measures);

        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("SELECT * FROM company");
        modelReq.setModelDetail(modelDetail);
        ModelResp companyModel = modelService.createModel(modelReq, defaultUser);

        enableDimensionValue(getDimension("company_name", companyModel));
        enableDimensionValue(getDimension("founder", companyModel));
        enableDimensionValue(getDimension("ceo", companyModel));

        return companyModel;
    }

    public ModelResp addModel_2(DomainResp domain, DatabaseResp database) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("品牌维度");
        modelReq.setBizName("brand");
        modelReq.setDatabaseId(database.getId());
        modelReq.setDomainId(domain.getId());
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Dimension> dimensions = new ArrayList<>();
        modelDetail.setDimensions(dimensions);

        dimensions.add(new Dimension("品牌名称", "brand_name", DimensionType.categorical, 1));
        dimensions.add(new Dimension("品牌成立时间", "brand_established_time", DimensionType.time, 1));
        dimensions
                .add(new Dimension("法定代表人", "legal_representative", DimensionType.categorical, 1));

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("品牌id", IdentifyType.primary.name(), "brand_id"));
        identifiers.add(new Identify("公司id", IdentifyType.foreign.name(), "company_id"));
        modelDetail.setIdentifiers(identifiers);

        List<Measure> measures = new ArrayList<>();
        measures.add(new Measure("注册资本", "registered_capital", AggOperatorEnum.MAX.name(), 1));
        modelDetail.setMeasures(measures);

        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("SELECT * FROM brand");
        modelReq.setModelDetail(modelDetail);
        ModelResp brandModel = modelService.createModel(modelReq, defaultUser);

        enableDimensionValue(getDimension("brand_name", brandModel));

        return brandModel;
    }

    public ModelResp addModel_3(DomainResp domain, DatabaseResp database) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("品牌历年收入");
        modelReq.setBizName("brand_revenue");
        modelReq.setDatabaseId(database.getId());
        modelReq.setDomainId(domain.getId());
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Dimension> dimensions = new ArrayList<>();
        modelDetail.setDimensions(dimensions);

        dimensions.add(new Dimension("财年", "year_time", DimensionType.time, 1, "year_time", "yyyy",
                new DimensionTimeTypeParams("false", "year")));

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("品牌id", IdentifyType.foreign.name(), "brand_id"));
        modelDetail.setIdentifiers(identifiers);

        List<Measure> measures = new ArrayList<>();
        measures.add(new Measure("营收", "revenue", AggOperatorEnum.SUM.name(), 1));
        measures.add(new Measure("利润", "profit", AggOperatorEnum.SUM.name(), 1));
        measures.add(new Measure("营收同比增长", "revenue_growth_year_on_year",
                AggOperatorEnum.AVG.name(), 1));
        measures.add(
                new Measure("利润同比增长", "profit_growth_year_on_year", AggOperatorEnum.AVG.name(), 1));
        modelDetail.setMeasures(measures);

        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("SELECT * FROM brand_revenue");
        modelReq.setModelDetail(modelDetail);
        return modelService.createModel(modelReq, defaultUser);
    }

    public DataSetResp addDataSet(DomainResp domain) {
        DataSetReq dataSetReq = new DataSetReq();
        dataSetReq.setName("企业数据集");
        dataSetReq.setBizName("CorporateData");
        dataSetReq.setDomainId(domain.getId());
        dataSetReq.setDescription("巨头公司核心经营数据");
        dataSetReq.setAdmins(Lists.newArrayList("admin"));

        List<DataSetModelConfig> dataSetModelConfigs = getDataSetModelConfigs(domain.getId());
        DataSetDetail dataSetDetail = new DataSetDetail();
        dataSetDetail.setDataSetModelConfigs(dataSetModelConfigs);
        dataSetReq.setDataSetDetail(dataSetDetail);
        dataSetReq.setTypeEnum(TypeEnums.DATASET);

        QueryConfig queryConfig = new QueryConfig();
        AggregateTypeDefaultConfig aggregateTypeDefaultConfig = new AggregateTypeDefaultConfig();
        TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();
        timeDefaultConfig.setTimeMode(TimeMode.LAST);
        timeDefaultConfig.setUnit(1);
        aggregateTypeDefaultConfig.setTimeDefaultConfig(timeDefaultConfig);
        queryConfig.setAggregateTypeDefaultConfig(aggregateTypeDefaultConfig);
        dataSetReq.setQueryConfig(queryConfig);

        return dataSetService.save(dataSetReq, defaultUser);
    }

    public void addModelRela(DomainResp domain, ModelResp fromModel, ModelResp toModel,
            String joinField) {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition(joinField, joinField, FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(domain.getId());
        modelRelaReq.setFromModelId(fromModel.getId());
        modelRelaReq.setToModelId(toModel.getId());
        modelRelaReq.setJoinType("inner join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, defaultUser);
    }

    private Agent addAgent(Long dataSetId) {
        Agent agent = new Agent();
        agent.setName(AGENT_NAME);
        agent.setDescription("帮忙您对企业的员工数、收入、利润经营指标分析");
        agent.setStatus(1);
        agent.setEnableSearch(1);
        agent.setExamples(
                Lists.newArrayList("各公司员工都有多少人", "利润最高的品牌top 3", "2024财年aws的营收和利润", "特斯拉下有哪些品牌"));
        ToolConfig toolConfig = new ToolConfig();

        // configure tools
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

        return agentService.createAgent(agent, defaultUser);
    }
}
