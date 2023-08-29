package com.tencent.supersonic;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.agent.Agent;
import com.tencent.supersonic.chat.agent.AgentConfig;
import com.tencent.supersonic.chat.agent.tool.AgentToolType;
import com.tencent.supersonic.chat.agent.tool.RuleQueryTool;
import com.tencent.supersonic.chat.api.pojo.request.ChatAggConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigBaseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatDefaultConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatDetailConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.request.ItemVisibility;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.request.RecommendedQuestionReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.query.plugin.ParamOption;
import com.tencent.supersonic.chat.query.plugin.WebBase;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.service.PluginService;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.common.util.JsonUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConfigureDemo implements ApplicationListener<ApplicationReadyEvent> {
    private User user = User.getFakeUser();
    @Qualifier("chatQueryService")
    @Autowired
    private QueryService queryService;
    @Autowired
    private ChatService chatService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private PluginService pluginService;
    @Autowired
    private AgentService agentService;

    private void parseAndExecute(int chatId, String queryText) throws Exception {
        QueryReq queryRequest = new QueryReq();
        queryRequest.setQueryText(queryText);
        queryRequest.setChatId(chatId);
        queryRequest.setAgentId(1);
        queryRequest.setUser(User.getFakeUser());
        ParseResp parseResp = queryService.performParsing(queryRequest);

        ExecuteQueryReq executeReq = new ExecuteQueryReq();
        executeReq.setQueryId(parseResp.getQueryId());
        executeReq.setParseId(parseResp.getSelectedParses().get(0).getId());
        executeReq.setQueryText(queryRequest.getQueryText());
        executeReq.setParseInfo(parseResp.getSelectedParses().get(0));
        executeReq.setChatId(parseResp.getChatId());
        executeReq.setUser(queryRequest.getUser());
        executeReq.setAgentId(1);
        queryService.performExecution(executeReq);
    }

    public void addSampleChats() throws Exception {
        chatService.addChat(user, "样例对话1");

        parseAndExecute(1, "超音数 访问次数");
        parseAndExecute(1, "按部门统计");
        parseAndExecute(1, "查询近30天");
    }

    public void addSampleChats2() throws Exception {
        chatService.addChat(user, "样例对话2");

        parseAndExecute(2, "alice 停留时长");
        parseAndExecute(2, "对比alice和lucy的访问次数");
        parseAndExecute(2, "访问次数最高的部门");
    }

    public void addDemoChatConfig_1() {
        ChatConfigBaseReq chatConfigBaseReq = new ChatConfigBaseReq();
        chatConfigBaseReq.setModelId(1L);

        ChatDetailConfigReq chatDetailConfig = new ChatDetailConfigReq();
        ChatDefaultConfigReq chatDefaultConfigDetail = new ChatDefaultConfigReq();
        List<Long> dimensionIds0 = Arrays.asList(1L, 2L);
        List<Long> metricIds0 = Arrays.asList(1L);
        chatDefaultConfigDetail.setDimensionIds(dimensionIds0);
        chatDefaultConfigDetail.setMetricIds(metricIds0);
        chatDefaultConfigDetail.setUnit(7);
        chatDefaultConfigDetail.setPeriod("DAY");
        chatDetailConfig.setChatDefaultConfig(chatDefaultConfigDetail);
        ItemVisibility visibility0 = new ItemVisibility();
        chatDetailConfig.setVisibility(visibility0);
        chatConfigBaseReq.setChatDetailConfig(chatDetailConfig);


        ChatAggConfigReq chatAggConfig = new ChatAggConfigReq();
        ChatDefaultConfigReq chatDefaultConfigAgg = new ChatDefaultConfigReq();
        List<Long> dimensionIds1 = Arrays.asList(1L, 2L);
        List<Long> metricIds1 = Arrays.asList(1L);
        chatDefaultConfigAgg.setDimensionIds(dimensionIds1);
        chatDefaultConfigAgg.setMetricIds(metricIds1);
        chatDefaultConfigAgg.setUnit(7);
        chatDefaultConfigAgg.setPeriod("DAY");
        chatDefaultConfigAgg.setTimeMode(ChatDefaultConfigReq.TimeMode.RECENT);
        chatAggConfig.setChatDefaultConfig(chatDefaultConfigAgg);
        ItemVisibility visibility1 = new ItemVisibility();
        chatAggConfig.setVisibility(visibility1);
        chatConfigBaseReq.setChatAggConfig(chatAggConfig);

        List<RecommendedQuestionReq> recommendedQuestions = new ArrayList<>();
        RecommendedQuestionReq recommendedQuestionReq0 = new RecommendedQuestionReq("超音数访问次数");
        RecommendedQuestionReq recommendedQuestionReq1 = new RecommendedQuestionReq("超音数访问人数");
        RecommendedQuestionReq recommendedQuestionReq2 = new RecommendedQuestionReq("超音数按部门访问次数");
        recommendedQuestions.add(recommendedQuestionReq0);
        recommendedQuestions.add(recommendedQuestionReq1);
        recommendedQuestions.add(recommendedQuestionReq2);
        chatConfigBaseReq.setRecommendedQuestions(recommendedQuestions);

        configService.addConfig(chatConfigBaseReq, user);
    }

    public void addDemoChatConfig_2() {
        ChatConfigBaseReq chatConfigBaseReq = new ChatConfigBaseReq();
        chatConfigBaseReq.setModelId(2L);

        ChatDetailConfigReq chatDetailConfig = new ChatDetailConfigReq();
        ChatDefaultConfigReq chatDefaultConfigDetail = new ChatDefaultConfigReq();
        List<Long> dimensionIds0 = Arrays.asList(4L, 5L, 6L, 7L);
        List<Long> metricIds0 = Arrays.asList(4L);
        chatDefaultConfigDetail.setDimensionIds(dimensionIds0);
        chatDefaultConfigDetail.setMetricIds(metricIds0);
        chatDefaultConfigDetail.setUnit(7);
        chatDefaultConfigDetail.setPeriod("DAY");
        chatDetailConfig.setChatDefaultConfig(chatDefaultConfigDetail);
        ItemVisibility visibility0 = new ItemVisibility();
        chatDetailConfig.setVisibility(visibility0);
        chatConfigBaseReq.setChatDetailConfig(chatDetailConfig);


        ChatAggConfigReq chatAggConfig = new ChatAggConfigReq();
        ChatDefaultConfigReq chatDefaultConfigAgg = new ChatDefaultConfigReq();
        List<Long> dimensionIds1 = Arrays.asList(4L, 5L, 6L, 7L);
        List<Long> metricIds1 = Arrays.asList(4L);
        chatDefaultConfigAgg.setDimensionIds(dimensionIds1);
        chatDefaultConfigAgg.setMetricIds(metricIds1);
        chatDefaultConfigAgg.setUnit(7);
        chatDefaultConfigAgg.setPeriod("DAY");
        chatDefaultConfigAgg.setTimeMode(ChatDefaultConfigReq.TimeMode.RECENT);
        chatAggConfig.setChatDefaultConfig(chatDefaultConfigAgg);
        ItemVisibility visibility1 = new ItemVisibility();
        chatAggConfig.setVisibility(visibility1);
        chatConfigBaseReq.setChatAggConfig(chatAggConfig);

        List<RecommendedQuestionReq> recommendedQuestions = new ArrayList<>();
        chatConfigBaseReq.setRecommendedQuestions(recommendedQuestions);

        configService.addConfig(chatConfigBaseReq, user);
    }

    private void addPlugin_1() {
        Plugin plugin1 = new Plugin();
        plugin1.setType("WEB_PAGE");
        plugin1.setModelList(Arrays.asList(1L));
        plugin1.setPattern("访问情况");
        plugin1.setParseModeConfig(null);
        plugin1.setName("访问情况");
        WebBase webBase = new WebBase();
        webBase.setUrl("www.test.com");
        ParamOption paramOption = new ParamOption();
        paramOption.setKey("name");
        paramOption.setParamType(ParamOption.ParamType.SEMANTIC);
        paramOption.setElementId(2L);
        paramOption.setModelId(1L);
        List<ParamOption> paramOptions = Arrays.asList(paramOption);
        webBase.setParamOptions(paramOptions);
        plugin1.setConfig(JsonUtil.toString(webBase));

        pluginService.createPlugin(plugin1, user);
    }

    private void addAgent() {
        Agent agent = new Agent();
        agent.setId(1);
        agent.setName("查指标");
        agent.setDescription("查指标");
        agent.setStatus(1);
        agent.setEnableSearch(1);
        agent.setExamples(Lists.newArrayList("超音数访问次数", "超音数访问人数", "alice 停留时长"));
        AgentConfig agentConfig = new AgentConfig();
        RuleQueryTool ruleQueryTool = new RuleQueryTool();
        ruleQueryTool.setType(AgentToolType.RULE);
        ruleQueryTool.setModelIds(Lists.newArrayList(1L, 2L));
        ruleQueryTool.setQueryModes(Lists.newArrayList(
                "ENTITY_DETAIL", "ENTITY_LIST_FILTER", "ENTITY_ID", "METRIC_ENTITY",
                "METRIC_FILTER", "METRIC_GROUPBY", "METRIC_MODEL", "METRIC_ORDERBY"
        ));
        agentConfig.getTools().add(ruleQueryTool);
        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        agentService.createAgent(agent, User.getFakeUser());
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        try {
            addDemoChatConfig_1();
            addDemoChatConfig_2();
            addPlugin_1();
            addAgent();
            addSampleChats();
            addSampleChats2();
        } catch (Exception e) {
            log.error("Failed to add sample chats", e);
        }
    }


}
