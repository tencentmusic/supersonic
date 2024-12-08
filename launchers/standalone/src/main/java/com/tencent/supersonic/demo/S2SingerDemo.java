package com.tencent.supersonic.demo;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.DatasetTool;
import com.tencent.supersonic.chat.server.agent.ToolConfig;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.headless.api.pojo.AggregateTypeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.DataSetDetail;
import com.tencent.supersonic.headless.api.pojo.DataSetModelConfig;
import com.tencent.supersonic.headless.api.pojo.DetailTypeDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.Dimension;
import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.ModelDetail;
import com.tencent.supersonic.headless.api.pojo.QueryConfig;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@Order(3)
public class S2SingerDemo extends S2BaseDemo {

    public static final String AGENT_NAME = "艺人分析助手";

    public void doRun() {
        try {
            DomainResp singerDomain = addDomain();
            ModelResp singerModel = addModel(singerDomain, demoDatabase);
            long dataSetId = addDataSet(singerDomain, singerModel);
            addAgent(dataSetId);
        } catch (Exception e) {
            log.error("Failed to add model demo data", e);
        }
    }

    @Override
    protected boolean checkNeedToRun() {
        List<DomainResp> domainList = domainService.getDomainList();
        for (DomainResp domainResp : domainList) {
            if (domainResp.getBizName().equalsIgnoreCase("singer")) {
                log.info("Already exist domain:singer, no need to run demo");
                return false;
            }
        }
        return true;
    }

    public DomainResp addDomain() {
        DomainReq domainReq = new DomainReq();
        domainReq.setName("歌手数据域");
        domainReq.setBizName("singer");
        domainReq.setParentId(0L);
        domainReq.setStatus(StatusEnum.ONLINE.getCode());
        domainReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        domainReq.setViewOrgs(Collections.singletonList("1"));
        domainReq.setAdmins(Arrays.asList("admin", "alice"));
        domainReq.setAdminOrgs(Collections.emptyList());
        domainReq.setIsOpen(1);
        return domainService.createDomain(domainReq, defaultUser);
    }

    public ModelResp addModel(DomainResp singerDomain, DatabaseResp s2Database) throws Exception {
        ModelReq modelReq = new ModelReq();
        modelReq.setName("歌手库");
        modelReq.setBizName("singer");
        modelReq.setDescription("歌手库");
        modelReq.setDatabaseId(s2Database.getId());
        modelReq.setDomainId(singerDomain.getId());
        modelReq.setViewers(Arrays.asList("admin", "tom", "jack"));
        modelReq.setViewOrgs(Collections.singletonList("1"));
        modelReq.setAdmins(Collections.singletonList("admin"));
        modelReq.setAdminOrgs(Collections.emptyList());
        ModelDetail modelDetail = new ModelDetail();
        List<Identify> identifiers = new ArrayList<>();
        Identify identify = new Identify("歌手名", IdentifyType.primary.name(), "singer_name", 1);
        identifiers.add(identify);
        modelDetail.setIdentifiers(identifiers);

        List<Dimension> dimensions = new ArrayList<>();
        dimensions.add(new Dimension("活跃区域", "act_area", DimensionType.categorical, 1));
        dimensions.add(new Dimension("代表作", "song_name", DimensionType.categorical, 1));
        dimensions.add(new Dimension("流派", "genre", DimensionType.categorical, 1));
        modelDetail.setDimensions(dimensions);

        Measure measure1 = new Measure("播放量", "js_play_cnt", "sum", 1);
        Measure measure2 = new Measure("下载量", "down_cnt", "sum", 1);
        Measure measure3 = new Measure("收藏量", "favor_cnt", "sum", 1);
        modelDetail.setMeasures(Lists.newArrayList(measure1, measure2, measure3));
        modelDetail.setQueryType("sql_query");
        modelDetail.setSqlQuery("select singer_name, act_area, song_name, genre, "
                + "js_play_cnt, down_cnt, favor_cnt from singer");
        modelReq.setModelDetail(modelDetail);
        ModelResp modelResp = modelService.createModel(modelReq, defaultUser);

        // create dict conf for dimensions
        enableDimensionValue(getDimension("act_area", modelResp));
        enableDimensionValue(getDimension("genre", modelResp));
        enableDimensionValue(getDimension("singer_name", modelResp));

        return modelResp;
    }

    public long addDataSet(DomainResp singerDomain, ModelResp singerModel) {
        DataSetReq dataSetReq = new DataSetReq();
        dataSetReq.setName("歌手数据集");
        dataSetReq.setBizName("singer");
        dataSetReq.setDomainId(singerDomain.getId());
        dataSetReq.setDescription("包含歌手相关标签和指标信息");
        dataSetReq.setAdmins(Lists.newArrayList("admin", "jack"));
        List<DataSetModelConfig> dataSetModelConfigs = getDataSetModelConfigs(singerDomain.getId());
        DataSetDetail dataSetDetail = new DataSetDetail();
        dataSetDetail.setDataSetModelConfigs(dataSetModelConfigs);
        dataSetReq.setDataSetDetail(dataSetDetail);
        dataSetReq.setTypeEnum(TypeEnums.DATASET);
        QueryConfig queryConfig = new QueryConfig();
        DetailTypeDefaultConfig detailTypeDefaultConfig = new DetailTypeDefaultConfig();
        AggregateTypeDefaultConfig aggregateTypeDefaultConfig = new AggregateTypeDefaultConfig();
        queryConfig.setDetailTypeDefaultConfig(detailTypeDefaultConfig);
        queryConfig.setAggregateTypeDefaultConfig(aggregateTypeDefaultConfig);
        dataSetReq.setQueryConfig(queryConfig);
        DataSetResp dataSetResp = dataSetService.save(dataSetReq, defaultUser);
        return dataSetResp.getId();
    }

    private void addAgent(long dataSetId) {
        Agent agent = new Agent();
        agent.setName(AGENT_NAME);
        agent.setDescription("帮忙您对不同流派、区域的艺人做分析查询");
        agent.setStatus(1);
        agent.setEnableSearch(1);
        agent.setExamples(Lists.newArrayList("国风流派歌手", "港台歌手", "周杰伦流派"));

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
        agent.setAdmins(Lists.newArrayList("alice"));
        agent.setViewers(Lists.newArrayList("tom", "jack"));
        agentService.createAgent(agent, defaultUser);
    }
}
