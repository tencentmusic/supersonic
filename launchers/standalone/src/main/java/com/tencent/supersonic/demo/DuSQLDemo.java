package com.tencent.supersonic.demo;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentConfig;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.LLMParserTool;
import com.tencent.supersonic.common.pojo.JoinCondition;
import com.tencent.supersonic.common.pojo.ModelRela;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.enums.DimensionType;
import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.DimensionTimeTypeParams;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.ModelDetail;
import com.tencent.supersonic.headless.api.pojo.DataSetDetail;
import com.tencent.supersonic.headless.api.pojo.DataSetModelConfig;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.MetricTypeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.request.DomainReq;
import com.tencent.supersonic.headless.api.pojo.request.MetricReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelReq;
import com.tencent.supersonic.headless.api.pojo.request.DataSetReq;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class DuSQLDemo extends S2BaseDemo {

    public void doRun() {
        try {
            addDomain();
            addModel_1();
            addModel_2();
            addModel_3();
            addModel_4();
            addDataSet_1();
            addModelRela_1();
            addModelRela_2();
            addModelRela_3();
            addModelRela_4();
            addAgent();
        } catch (Exception e) {
            log.error("Failed to add bench mark demo data", e);
        }

    }

    @Override
    boolean checkNeedToRun() {
        return false;
    }

    public void addDomain() {
        DomainReq domainReq = new DomainReq();
        domainReq.setName("DuSQL_互联网企业");
        domainReq.setBizName("internet");
        domainReq.setParentId(0L);
        domainReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        domainReq.setViewOrgs(Collections.singletonList("1"));
        domainReq.setAdmins(Collections.singletonList("admin"));
        domainReq.setAdminOrgs(Collections.emptyList());
        domainService.createDomain(domainReq, user);
    }

    //9
    public void addModel_1() throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("公司");
        modelReq.setBizName("company");
        modelReq.setDescription("公司");
        modelReq.setDatabaseId(1L);
        modelReq.setDomainId(4L);
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionType.time.name(), 0);
        DimensionTimeTypeParams dimensionTimeTypeParams = new DimensionTimeTypeParams("false", "none");
        dimension1.setTypeParams(dimensionTimeTypeParams);
        dimensions.add(dimension1);
        dimensions.add(new Dim("公司名称", "company_name", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("总部地点", "headquarter_address", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("公司成立时间", "company_established_time", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("创始人", "founder", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("首席执行官", "ceo", DimensionType.categorical.name(), 1));
        modelDetail.setDimensions(dimensions);

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("公司id", IdentifyType.primary.name(), "company_id"));
        modelDetail.setIdentifiers(identifiers);

        List<Measure> measures = new ArrayList<>();
        measures.add(new Measure("年营业额", "annual_turnover", AggOperatorEnum.SUM.name(), 1));
        Measure measure = new Measure("员工数", "employee_count", AggOperatorEnum.SUM.name(), 1);
        measures.add(measure);
        modelDetail.setMeasures(measures);

        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("SELECT imp_date,company_id,company_name,headquarter_address,"
                + "company_established_time,founder,ceo,annual_turnover,employee_count FROM company");
        modelReq.setModelDetail(modelDetail);
        modelService.createModel(modelReq, user);
    }

    // 10
    public void addModel_2() throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("品牌");
        modelReq.setBizName("brand");
        modelReq.setDescription("品牌");
        modelReq.setDatabaseId(1L);
        modelReq.setDomainId(4L);
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionType.time.name(), 0);
        DimensionTimeTypeParams dimensionTimeTypeParams = new DimensionTimeTypeParams("false", "none");
        dimension1.setTypeParams(dimensionTimeTypeParams);
        dimensions.add(dimension1);
        dimensions.add(new Dim("品牌名称", "brand_name", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("品牌成立时间", "brand_established_time", DimensionType.categorical.name(), 1));
        dimensions.add(new Dim("法定代表人", "legal_representative", DimensionType.categorical.name(), 1));
        modelDetail.setDimensions(dimensions);

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("品牌id", IdentifyType.primary.name(), "brand_id"));
        identifiers.add(new Identify("公司id", IdentifyType.foreign.name(), "company_id"));
        modelDetail.setIdentifiers(identifiers);

        List<Measure> measures = new ArrayList<>();
        measures.add(new Measure("注册资本", "registered_capital", AggOperatorEnum.SUM.name(), 1));
        modelDetail.setMeasures(measures);

        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("SELECT  imp_date,brand_id,brand_name,brand_established_time,"
                + "company_id,legal_representative,registered_capital FROM brand");
        modelReq.setModelDetail(modelDetail);
        modelService.createModel(modelReq, user);
    }

    // 11
    public void addModel_3() throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("公司各品牌收入排名");
        modelReq.setBizName("company_revenue");
        modelReq.setDescription("公司各品牌收入排名");
        modelReq.setDatabaseId(1L);
        modelReq.setDomainId(4L);
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionType.time.name(), 0);
        DimensionTimeTypeParams dimensionTimeTypeParams = new DimensionTimeTypeParams("false", "none");
        dimension1.setTypeParams(dimensionTimeTypeParams);
        dimensions.add(dimension1);
        modelDetail.setDimensions(dimensions);

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("公司id", IdentifyType.foreign.name(), "company_id"));
        identifiers.add(new Identify("品牌id", IdentifyType.foreign.name(), "brand_id"));
        modelDetail.setIdentifiers(identifiers);

        List<Measure> measures = new ArrayList<>();
        Measure measure = new Measure("营收占比", "revenue_proportion", AggOperatorEnum.SUM.name(), 1);
        measures.add(measure);
        measures.add(new Measure("利润占比", "profit_proportion", AggOperatorEnum.SUM.name(), 1));
        measures.add(new Measure("支出占比", "expenditure_proportion", AggOperatorEnum.SUM.name(), 1));
        modelDetail.setMeasures(measures);

        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("SELECT imp_date,company_id,brand_id,revenue_proportion,"
                + "profit_proportion,expenditure_proportion FROM company_revenue");
        modelReq.setModelDetail(modelDetail);
        modelService.createModel(modelReq, user);
        MetricResp metricResp = metricService.getMetric(13L, user);

        MetricReq metricReq = new MetricReq();
        BeanUtils.copyProperties(metricResp, metricReq);
        metricReq.setAlias("收入比例");
        metricService.updateMetric(metricReq, user);
    }

    // 12
    public void addModel_4() throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("公司品牌历年收入");
        modelReq.setBizName("company_brand_revenue");
        modelReq.setDescription("公司品牌历年收入");
        modelReq.setDatabaseId(1L);
        modelReq.setDomainId(4L);
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Dim> dimensions = new ArrayList<>();
        Dim dimension1 = new Dim("", "imp_date", DimensionType.time.name(), 0);
        DimensionTimeTypeParams dimensionTimeTypeParams = new DimensionTimeTypeParams("false", "none");
        dimension1.setTypeParams(dimensionTimeTypeParams);
        dimensions.add(dimension1);
        dimensions.add(new Dim("年份", "year_time", DimensionType.categorical.name(), 1));
        modelDetail.setDimensions(dimensions);

        List<Identify> identifiers = new ArrayList<>();
        identifiers.add(new Identify("品牌id", IdentifyType.foreign.name(), "brand_id"));
        modelDetail.setIdentifiers(identifiers);

        List<Measure> measures = new ArrayList<>();
        measures.add(new Measure("营收", "revenue", AggOperatorEnum.SUM.name(), 1));
        measures.add(new Measure("利润", "profit", AggOperatorEnum.SUM.name(), 1));
        measures.add(new Measure("营收同比增长", "revenue_growth_year_on_year", AggOperatorEnum.SUM.name(), 1));
        measures.add(new Measure("利润同比增长", "profit_growth_year_on_year", AggOperatorEnum.SUM.name(), 1));
        modelDetail.setMeasures(measures);

        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("SELECT imp_date,year_time,brand_id,revenue,profit,"
                + "revenue_growth_year_on_year,profit_growth_year_on_year FROM company_brand_revenue");
        modelReq.setModelDetail(modelDetail);
        modelService.createModel(modelReq, user);

    }

    public void addDataSet_1() {
        DataSetReq dataSetReq = new DataSetReq();
        dataSetReq.setName("DuSQL 互联网企业");
        dataSetReq.setBizName("internet");
        dataSetReq.setDomainId(4L);
        dataSetReq.setDescription("DuSQL互联网企业数据源相关的指标和维度等");
        dataSetReq.setAdmins(Lists.newArrayList("admin"));
        List<DataSetModelConfig> viewModelConfigs = Lists.newArrayList(
                new DataSetModelConfig(9L, Lists.newArrayList(16L, 17L, 18L, 19L, 20L), Lists.newArrayList(10L, 11L)),
                new DataSetModelConfig(10L, Lists.newArrayList(21L, 22L, 23L), Lists.newArrayList(12L)),
                new DataSetModelConfig(11L, Lists.newArrayList(), Lists.newArrayList(13L, 14L, 15L)),
                new DataSetModelConfig(12L, Lists.newArrayList(24L), Lists.newArrayList(16L, 17L, 18L, 19L)));

        DataSetDetail dsDetail = new DataSetDetail();
        dsDetail.setDataSetModelConfigs(viewModelConfigs);
        dataSetReq.setDataSetDetail(dsDetail);
        dataSetReq.setTypeEnum(TypeEnums.DATASET);
        QueryConfig queryConfig = new QueryConfig();
        MetricTypeDefaultConfig metricTypeDefaultConfig = new MetricTypeDefaultConfig();
        TimeDefaultConfig timeDefaultConfig = new TimeDefaultConfig();
        timeDefaultConfig.setTimeMode(TimeMode.RECENT);
        timeDefaultConfig.setUnit(1);
        metricTypeDefaultConfig.setTimeDefaultConfig(timeDefaultConfig);
        queryConfig.setMetricTypeDefaultConfig(metricTypeDefaultConfig);
        dataSetReq.setQueryConfig(queryConfig);
        dataSetService.save(dataSetReq, User.getFakeUser());
    }

    public void addModelRela_1() {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("company_id", "company_id", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(4L);
        modelRelaReq.setFromModelId(9L);
        modelRelaReq.setToModelId(10L);
        modelRelaReq.setJoinType("inner join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, user);
    }

    public void addModelRela_2() {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("company_id", "company_id", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(4L);
        modelRelaReq.setFromModelId(9L);
        modelRelaReq.setToModelId(11L);
        modelRelaReq.setJoinType("inner join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, user);
    }

    public void addModelRela_3() {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("brand_id", "brand_id", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(4L);
        modelRelaReq.setFromModelId(10L);
        modelRelaReq.setToModelId(11L);
        modelRelaReq.setJoinType("inner join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, user);
    }

    public void addModelRela_4() {
        List<JoinCondition> joinConditions = Lists.newArrayList();
        joinConditions.add(new JoinCondition("brand_id", "brand_id", FilterOperatorEnum.EQUALS));
        ModelRela modelRelaReq = new ModelRela();
        modelRelaReq.setDomainId(4L);
        modelRelaReq.setFromModelId(10L);
        modelRelaReq.setToModelId(12L);
        modelRelaReq.setJoinType("inner join");
        modelRelaReq.setJoinConditions(joinConditions);
        modelRelaService.save(modelRelaReq, user);
    }

    private void addAgent() {
        Agent agent = new Agent();
        agent.setName("DuSQL 互联网企业");
        agent.setDescription("DuSQL");
        agent.setStatus(1);
        agent.setEnableSearch(1);
        agent.setExamples(Lists.newArrayList());
        AgentConfig agentConfig = new AgentConfig();

        LLMParserTool llmParserTool = new LLMParserTool();
        llmParserTool.setId("1");
        llmParserTool.setType(AgentToolType.NL2SQL_LLM);
        llmParserTool.setDataSetIds(Lists.newArrayList(4L));
        agentConfig.getTools().add(llmParserTool);

        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        log.info("agent:{}", JsonUtil.toString(agent));
        agentService.createAgent(agent, User.getFakeUser());
    }

}
