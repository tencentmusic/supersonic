package com.tencent.supersonic.demo;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentConfig;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.LLMParserTool;
import com.tencent.supersonic.chat.server.agent.RuleParserTool;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.api.pojo.DataSetDetail;
import com.tencent.supersonic.headless.api.pojo.DataSetModelConfig;
import com.tencent.supersonic.headless.api.pojo.DefaultDisplayInfo;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.DimensionTimeTypeParams;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.MetricTypeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.ModelDetail;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
import com.tencent.supersonic.headless.api.pojo.TagTypeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.enums.DimensionType;
import com.tencent.supersonic.headless.api.pojo.enums.IdentifyType;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import com.tencent.supersonic.headless.api.pojo.request.DataSetReq;
import com.tencent.supersonic.headless.api.pojo.request.DomainReq;
import com.tencent.supersonic.headless.api.pojo.request.ModelReq;
import com.tencent.supersonic.headless.api.pojo.request.TagObjectReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.DomainResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.TagObjectResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@Order(2)
public class S2ArtistDemo extends S2BaseDemo {

    public void doRun() {
        try {
            DomainResp singerDomain = addDomain();
            TagObjectResp singerTagObject = addTagObjectSinger(singerDomain);
            ModelResp singerModel = addModel(singerDomain, demoDatabaseResp, singerTagObject);
            addTags(singerModel);
            long dataSetId = addDataSet(singerDomain, singerModel);
            addAgent(dataSetId);
        } catch (Exception e) {
            log.error("Failed to add model demo data", e);
        }
    }

    @Override
    boolean checkNeedToRun() {
        List<DomainResp> domainList = domainService.getDomainList();
        for (DomainResp domainResp : domainList) {
            if (domainResp.getBizName().equalsIgnoreCase("singer")) {
                log.info("Already exist domain:singer, no need to run demo");
                return false;
            }
        }
        return true;
    }

    private TagObjectResp addTagObjectSinger(DomainResp singerDomain) throws Exception {
        TagObjectReq tagObjectReq = new TagObjectReq();
        tagObjectReq.setDomainId(singerDomain.getId());
        tagObjectReq.setName("艺人");
        tagObjectReq.setBizName("singer");
        User user = User.getFakeUser();
        return tagObjectService.create(tagObjectReq, user);
    }

    public DomainResp addDomain() {
        DomainReq domainReq = new DomainReq();
        domainReq.setName("艺人库");
        domainReq.setBizName("singer");
        domainReq.setParentId(0L);
        domainReq.setStatus(StatusEnum.ONLINE.getCode());
        domainReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        domainReq.setViewOrgs(Collections.singletonList("1"));
        domainReq.setAdmins(Arrays.asList("admin", "alice"));
        domainReq.setAdminOrgs(Collections.emptyList());
        domainReq.setIsOpen(1);
        return domainService.createDomain(domainReq, user);
    }

    public ModelResp addModel(DomainResp singerDomain,
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

    private void addTags(ModelResp model) {
        addTag(dimensionService.getDimension("act_area", model.getId()).getId(),
                TagDefineType.DIMENSION);
        addTag(dimensionService.getDimension("song_name", model.getId()).getId(),
                TagDefineType.DIMENSION);
        addTag(dimensionService.getDimension("genre", model.getId()).getId(),
                TagDefineType.DIMENSION);
        addTag(dimensionService.getDimension("singer_name", model.getId()).getId(),
                TagDefineType.DIMENSION);
        addTag(metricService.getMetric(model.getId(), "js_play_cnt").getId(),
                TagDefineType.METRIC);
    }

    public long addDataSet(DomainResp singerDomain, ModelResp singerModel) {
        DataSetReq dataSetReq = new DataSetReq();
        dataSetReq.setName("艺人库数据集");
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
        DataSetResp dataSetResp = dataSetService.save(dataSetReq, User.getFakeUser());
        return dataSetResp.getId();
    }

    private void addAgent(long dataSetId) {
        Agent agent = new Agent();
        agent.setName("做圈选");
        agent.setDescription("帮助您用自然语言进行圈选，支持多条件组合筛选");
        agent.setStatus(1);
        agent.setEnableSearch(1);
        agent.setExamples(Lists.newArrayList("国风风格艺人", "港台地区的艺人", "风格为流行的艺人"));
        AgentConfig agentConfig = new AgentConfig();
        RuleParserTool ruleQueryTool = new RuleParserTool();
        ruleQueryTool.setId("0");
        ruleQueryTool.setType(AgentToolType.NL2SQL_RULE);
        ruleQueryTool.setDataSetIds(Lists.newArrayList(dataSetId));
        agentConfig.getTools().add(ruleQueryTool);

        if (demoEnableLlm) {
            LLMParserTool llmParserTool = new LLMParserTool();
            llmParserTool.setId("1");
            llmParserTool.setType(AgentToolType.NL2SQL_LLM);
            llmParserTool.setDataSetIds(Lists.newArrayList(dataSetId));
            agentConfig.getTools().add(llmParserTool);
        }
        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        agentService.createAgent(agent, User.getFakeUser());
    }

}
