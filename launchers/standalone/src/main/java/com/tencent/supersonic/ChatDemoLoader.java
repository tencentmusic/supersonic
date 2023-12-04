package com.tencent.supersonic;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.agent.Agent;
import com.tencent.supersonic.chat.agent.AgentConfig;
import com.tencent.supersonic.chat.agent.AgentToolType;
import com.tencent.supersonic.chat.agent.LLMParserTool;
import com.tencent.supersonic.chat.agent.RuleParserTool;
import com.tencent.supersonic.chat.api.pojo.request.ChatAggConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatConfigBaseReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatDefaultConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatDetailConfigReq;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.request.ItemVisibility;
import com.tencent.supersonic.chat.api.pojo.request.KnowledgeInfoReq;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.request.RecommendedQuestionReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginParseConfig;
import com.tencent.supersonic.chat.query.plugin.ParamOption;
import com.tencent.supersonic.chat.query.plugin.WebBase;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.service.PluginService;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.common.pojo.QueryType;
import com.tencent.supersonic.common.pojo.SysParameter;
import com.tencent.supersonic.common.service.SysParameterService;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class ChatDemoLoader {

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
    @Autowired
    private SysParameterService sysParameterService;

    public void doRun() {
        try {
            addSysParameter();
            addDemoChatConfig_1();
            addDemoChatConfig_2();
            addDemoChatConfig_3();
            addDemoChatConfig_4();
            addDemoChatConfig_5();
            addDemoChatConfig_6();
            addDemoChatConfig_7();
            addDemoChatConfig_8();
            addPlugin_1();
            addAgent1();
            addAgent2();
            addAgent3();
            addSampleChats();
            addSampleChats2();
        } catch (Exception e) {
            log.error("Failed to add sample chats", e);
        }
    }

    private void parseAndExecute(int chatId, String queryText) throws Exception {
        QueryReq queryRequest = new QueryReq();
        queryRequest.setQueryText(queryText);
        queryRequest.setChatId(chatId);
        queryRequest.setAgentId(1);
        queryRequest.setUser(User.getFakeUser());
        ParseResp parseResp = queryService.performParsing(queryRequest);

        ExecuteQueryReq executeReq = ExecuteQueryReq.builder().build();
        executeReq.setQueryId(parseResp.getQueryId());
        executeReq.setParseId(parseResp.getCandidateParses().get(0).getId());
        executeReq.setQueryText(queryRequest.getQueryText());
        executeReq.setParseInfo(parseResp.getCandidateParses().get(0));
        executeReq.setChatId(parseResp.getChatId());
        executeReq.setUser(queryRequest.getUser());
        executeReq.setAgentId(1);
        executeReq.setSaveAnswer(true);
        queryService.performExecution(executeReq);
    }

    public void addSampleChats() throws Exception {
        chatService.addChat(user, "样例对话1", 1);

        parseAndExecute(1, "超音数 访问次数");
        parseAndExecute(1, "按部门统计");
        parseAndExecute(1, "查询近30天");
    }

    public void addSampleChats2() throws Exception {
        chatService.addChat(user, "样例对话2", 1);

        parseAndExecute(2, "alice 停留时长");
        parseAndExecute(2, "对比alice和lucy的访问次数");
        parseAndExecute(2, "访问次数最高的部门");
    }

    public void addSysParameter() {
        SysParameter sysParameter = new SysParameter();
        sysParameter.setId(1);
        sysParameter.init();
        sysParameterService.save(sysParameter);
    }

    public void addDemoChatConfig_1() {
        ChatConfigBaseReq chatConfigBaseReq = new ChatConfigBaseReq();
        chatConfigBaseReq.setModelId(1L);

        ChatDetailConfigReq chatDetailConfig = new ChatDetailConfigReq();
        ChatDefaultConfigReq chatDefaultConfigDetail = new ChatDefaultConfigReq();
        List<Long> dimensionIds0 = Collections.singletonList(1L);
        List<Long> metricIds0 = Lists.newArrayList();
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
        List<Long> dimensionIds1 = Arrays.asList(1L);
        List<Long> metricIds1 = Lists.newArrayList();
        chatDefaultConfigAgg.setDimensionIds(dimensionIds1);
        chatDefaultConfigAgg.setMetricIds(metricIds1);
        chatDefaultConfigAgg.setUnit(7);
        chatDefaultConfigAgg.setPeriod("DAY");
        chatDefaultConfigAgg.setTimeMode(ChatDefaultConfigReq.TimeMode.RECENT);
        chatAggConfig.setChatDefaultConfig(chatDefaultConfigAgg);
        ItemVisibility visibility1 = new ItemVisibility();
        chatAggConfig.setVisibility(visibility1);
        List<KnowledgeInfoReq> knowledgeInfos = new ArrayList<>();
        KnowledgeInfoReq knowledgeInfoReq = new KnowledgeInfoReq();
        knowledgeInfoReq.setItemId(3L);
        knowledgeInfoReq.setSearchEnable(true);
        knowledgeInfos.add(knowledgeInfoReq);
        chatAggConfig.setKnowledgeInfos(knowledgeInfos);
        chatConfigBaseReq.setChatAggConfig(chatAggConfig);
        configService.addConfig(chatConfigBaseReq, user);
    }

    public void addDemoChatConfig_2() {
        ChatConfigBaseReq chatConfigBaseReq = new ChatConfigBaseReq();
        chatConfigBaseReq.setModelId(2L);

        ChatDetailConfigReq chatDetailConfig = new ChatDetailConfigReq();
        ChatDefaultConfigReq chatDefaultConfigDetail = new ChatDefaultConfigReq();
        List<Long> dimensionIds0 = Collections.singletonList(3L);
        List<Long> metricIds0 = Arrays.asList(1L, 2L);
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
        List<Long> dimensionIds1 = Arrays.asList(3L);
        List<Long> metricIds1 = Arrays.asList(1L, 2L);
        chatDefaultConfigAgg.setDimensionIds(dimensionIds1);
        chatDefaultConfigAgg.setMetricIds(metricIds1);
        chatDefaultConfigAgg.setUnit(7);
        chatDefaultConfigAgg.setPeriod("DAY");
        chatDefaultConfigAgg.setTimeMode(ChatDefaultConfigReq.TimeMode.RECENT);
        chatAggConfig.setChatDefaultConfig(chatDefaultConfigAgg);
        ItemVisibility visibility1 = new ItemVisibility();
        chatAggConfig.setVisibility(visibility1);
        List<KnowledgeInfoReq> knowledgeInfos = new ArrayList<>();
        KnowledgeInfoReq knowledgeInfoReq = new KnowledgeInfoReq();
        knowledgeInfoReq.setItemId(3L);
        knowledgeInfoReq.setSearchEnable(true);
        knowledgeInfos.add(knowledgeInfoReq);
        chatAggConfig.setKnowledgeInfos(knowledgeInfos);
        chatConfigBaseReq.setChatAggConfig(chatAggConfig);
        configService.addConfig(chatConfigBaseReq, user);
    }

    public void addDemoChatConfig_3() {
        ChatConfigBaseReq chatConfigBaseReq = new ChatConfigBaseReq();
        chatConfigBaseReq.setModelId(3L);

        ChatDetailConfigReq chatDetailConfig = new ChatDetailConfigReq();
        ChatDefaultConfigReq chatDefaultConfigDetail = new ChatDefaultConfigReq();
        List<Long> dimensionIds0 = Arrays.asList(4L, 5L);
        List<Long> metricIds0 = Arrays.asList(3L);
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
        List<Long> dimensionIds1 = Arrays.asList(4L, 5L);
        List<Long> metricIds1 = Arrays.asList(3L);
        chatDefaultConfigAgg.setDimensionIds(dimensionIds1);
        chatDefaultConfigAgg.setMetricIds(metricIds1);
        chatDefaultConfigAgg.setUnit(7);
        chatDefaultConfigAgg.setPeriod("DAY");
        chatDefaultConfigAgg.setTimeMode(ChatDefaultConfigReq.TimeMode.RECENT);
        chatAggConfig.setChatDefaultConfig(chatDefaultConfigAgg);
        ItemVisibility visibility1 = new ItemVisibility();
        chatAggConfig.setVisibility(visibility1);
        List<KnowledgeInfoReq> knowledgeInfos = new ArrayList<>();
        KnowledgeInfoReq knowledgeInfoReq = new KnowledgeInfoReq();
        knowledgeInfoReq.setItemId(5L);
        knowledgeInfoReq.setSearchEnable(true);
        knowledgeInfos.add(knowledgeInfoReq);
        chatAggConfig.setKnowledgeInfos(knowledgeInfos);
        chatConfigBaseReq.setChatAggConfig(chatAggConfig);
        configService.addConfig(chatConfigBaseReq, user);
    }

    public void addDemoChatConfig_4() {
        ChatConfigBaseReq chatConfigBaseReq = new ChatConfigBaseReq();
        chatConfigBaseReq.setModelId(4L);

        ChatDetailConfigReq chatDetailConfig = new ChatDetailConfigReq();
        ChatDefaultConfigReq chatDefaultConfigDetail = new ChatDefaultConfigReq();
        List<Long> dimensionIds0 = Arrays.asList(6L, 7L, 8L, 9L);
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
        List<Long> dimensionIds1 = Arrays.asList(6L, 7L, 8L, 9L);
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

    public void addDemoChatConfig_5() {
        ChatConfigBaseReq chatConfigBaseReq = new ChatConfigBaseReq();
        chatConfigBaseReq.setModelId(5L);

        ChatDetailConfigReq chatDetailConfig = getChatDetailConfigReq();
        List<KnowledgeInfoReq> knowledgeInfos = new ArrayList<>();
        KnowledgeInfoReq knowledgeInfoReq = new KnowledgeInfoReq();
        knowledgeInfoReq.setSearchEnable(true);
        knowledgeInfoReq.setItemId(10L);
        knowledgeInfoReq.setBizName("most_popular_in");
        knowledgeInfos.add(knowledgeInfoReq);

        KnowledgeInfoReq knowledgeInfoReq2 = new KnowledgeInfoReq();
        knowledgeInfoReq2.setSearchEnable(true);
        knowledgeInfoReq2.setItemId(11L);
        knowledgeInfoReq2.setBizName("g_name");
        knowledgeInfos.add(knowledgeInfoReq2);

        chatDetailConfig.setKnowledgeInfos(knowledgeInfos);
        chatConfigBaseReq.setChatDetailConfig(chatDetailConfig);
        List<RecommendedQuestionReq> recommendedQuestions = new ArrayList<>();
        chatConfigBaseReq.setRecommendedQuestions(recommendedQuestions);
        configService.addConfig(chatConfigBaseReq, user);
    }

    private ChatDetailConfigReq getChatDetailConfigReq() {
        ChatDetailConfigReq chatDetailConfig = new ChatDetailConfigReq();
        ChatDefaultConfigReq chatDefaultConfigDetail = new ChatDefaultConfigReq();
        chatDefaultConfigDetail.setUnit(-1);
        chatDefaultConfigDetail.setPeriod("DAY");
        chatDetailConfig.setChatDefaultConfig(chatDefaultConfigDetail);
        ItemVisibility visibility0 = new ItemVisibility();
        chatDetailConfig.setVisibility(visibility0);
        return chatDetailConfig;
    }

    public void addDemoChatConfig_6() {
        ChatConfigBaseReq chatConfigBaseReq = new ChatConfigBaseReq();
        chatConfigBaseReq.setModelId(6L);

        ChatDetailConfigReq chatDetailConfig = getChatDetailConfigReq();
        List<KnowledgeInfoReq> knowledgeInfos = new ArrayList<>();
        KnowledgeInfoReq knowledgeInfoReq = new KnowledgeInfoReq();
        knowledgeInfoReq.setSearchEnable(true);
        knowledgeInfoReq.setItemId(12L);
        knowledgeInfoReq.setBizName("country");
        knowledgeInfos.add(knowledgeInfoReq);

        KnowledgeInfoReq knowledgeInfoReq2 = new KnowledgeInfoReq();
        knowledgeInfoReq2.setSearchEnable(true);
        knowledgeInfoReq2.setItemId(13L);
        knowledgeInfoReq2.setBizName("gender");
        knowledgeInfos.add(knowledgeInfoReq2);

        chatDetailConfig.setKnowledgeInfos(knowledgeInfos);
        chatConfigBaseReq.setChatDetailConfig(chatDetailConfig);
        List<RecommendedQuestionReq> recommendedQuestions = new ArrayList<>();
        chatConfigBaseReq.setRecommendedQuestions(recommendedQuestions);
        configService.addConfig(chatConfigBaseReq, user);
    }

    public void addDemoChatConfig_7() {
        ChatConfigBaseReq chatConfigBaseReq = new ChatConfigBaseReq();
        chatConfigBaseReq.setModelId(7L);

        ChatDetailConfigReq chatDetailConfig = getChatDetailConfigReq();
        List<KnowledgeInfoReq> knowledgeInfos = new ArrayList<>();
        KnowledgeInfoReq knowledgeInfoReq = new KnowledgeInfoReq();
        knowledgeInfoReq.setSearchEnable(true);
        knowledgeInfoReq.setItemId(16L);
        knowledgeInfoReq.setBizName("formats");
        knowledgeInfos.add(knowledgeInfoReq);

        chatDetailConfig.setKnowledgeInfos(knowledgeInfos);
        chatConfigBaseReq.setChatDetailConfig(chatDetailConfig);
        List<RecommendedQuestionReq> recommendedQuestions = new ArrayList<>();
        chatConfigBaseReq.setRecommendedQuestions(recommendedQuestions);
        configService.addConfig(chatConfigBaseReq, user);
    }

    public void addDemoChatConfig_8() {
        ChatConfigBaseReq chatConfigBaseReq = new ChatConfigBaseReq();
        chatConfigBaseReq.setModelId(8L);

        ChatDetailConfigReq chatDetailConfig = getChatDetailConfigReq();
        List<KnowledgeInfoReq> knowledgeInfos = new ArrayList<>();
        KnowledgeInfoReq knowledgeInfoReq = new KnowledgeInfoReq();
        knowledgeInfoReq.setSearchEnable(true);
        knowledgeInfoReq.setItemId(18L);
        knowledgeInfoReq.setBizName("country");
        knowledgeInfos.add(knowledgeInfoReq);

        KnowledgeInfoReq knowledgeInfoReq2 = new KnowledgeInfoReq();
        knowledgeInfoReq2.setSearchEnable(true);
        knowledgeInfoReq2.setItemId(19L);
        knowledgeInfoReq2.setBizName("languages");
        knowledgeInfos.add(knowledgeInfoReq2);

        KnowledgeInfoReq knowledgeInfoReq3 = new KnowledgeInfoReq();
        knowledgeInfoReq3.setSearchEnable(true);
        knowledgeInfoReq3.setItemId(21L);
        knowledgeInfoReq3.setBizName("song_name");
        knowledgeInfos.add(knowledgeInfoReq3);

        chatDetailConfig.setKnowledgeInfos(knowledgeInfos);
        chatConfigBaseReq.setChatDetailConfig(chatDetailConfig);
        List<RecommendedQuestionReq> recommendedQuestions = new ArrayList<>();
        chatConfigBaseReq.setRecommendedQuestions(recommendedQuestions);
        configService.addConfig(chatConfigBaseReq, user);
    }

    private void addPlugin_1() {
        Plugin plugin1 = new Plugin();
        plugin1.setType("WEB_PAGE");
        plugin1.setModelList(Arrays.asList(1L));
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
        paramOption.setElementId(2L);
        paramOption.setModelId(1L);
        List<ParamOption> paramOptions = Arrays.asList(paramOption);
        webBase.setParamOptions(paramOptions);
        plugin1.setConfig(JsonUtil.toString(webBase));

        pluginService.createPlugin(plugin1, user);
    }

    private void addAgent1() {
        Agent agent = new Agent();
        agent.setId(1);
        agent.setName("算指标");
        agent.setDescription("帮助您用自然语言查询指标，支持时间限定、条件筛选、下钻维度以及聚合统计");
        agent.setStatus(1);
        agent.setEnableSearch(1);
        agent.setExamples(Lists.newArrayList("超音数访问次数", "近15天超音数访问次数汇总", "按部门统计超音数的访问人数",
                "对比alice和lucy的停留时长", "超音数访问次数最高的部门"));
        AgentConfig agentConfig = new AgentConfig();
        RuleParserTool ruleQueryTool = new RuleParserTool();
        ruleQueryTool.setType(AgentToolType.NL2SQL_RULE);
        ruleQueryTool.setId("0");
        ruleQueryTool.setModelIds(Lists.newArrayList(-1L));
        ruleQueryTool.setQueryTypes(Lists.newArrayList(QueryType.METRIC.name()));
        agentConfig.getTools().add(ruleQueryTool);

        LLMParserTool llmParserTool = new LLMParserTool();
        llmParserTool.setId("1");
        llmParserTool.setType(AgentToolType.NL2SQL_LLM);
        llmParserTool.setModelIds(Lists.newArrayList(-1L));
        agentConfig.getTools().add(llmParserTool);

        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        agentService.createAgent(agent, User.getFakeUser());
    }

    private void addAgent2() {
        Agent agent = new Agent();
        agent.setId(2);
        agent.setName("标签圈选");
        agent.setDescription("帮助您用自然语言进行圈选，支持多条件组合筛选");
        agent.setStatus(1);
        agent.setEnableSearch(1);
        agent.setExamples(Lists.newArrayList("国风风格艺人", "港台地区的艺人", "风格为流行的艺人"));
        AgentConfig agentConfig = new AgentConfig();
        RuleParserTool ruleQueryTool = new RuleParserTool();
        ruleQueryTool.setId("0");
        ruleQueryTool.setType(AgentToolType.NL2SQL_RULE);
        ruleQueryTool.setModelIds(Lists.newArrayList(-1L));
        ruleQueryTool.setQueryTypes(Lists.newArrayList(QueryType.TAG.name()));
        agentConfig.getTools().add(ruleQueryTool);

        LLMParserTool llmParserTool = new LLMParserTool();
        llmParserTool.setId("1");
        llmParserTool.setType(AgentToolType.NL2SQL_LLM);
        llmParserTool.setModelIds(Lists.newArrayList(-1L));
        agentConfig.getTools().add(llmParserTool);

        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        agentService.createAgent(agent, User.getFakeUser());
    }

    private void addAgent3() {
        Agent agent = new Agent();
        agent.setId(3);
        agent.setName("cspider");
        agent.setDescription("cspider数据集的case展示");
        agent.setStatus(1);
        agent.setEnableSearch(1);
        agent.setExamples(Lists.newArrayList("可用“mp4”格式且分辨率低于1000的歌曲的ID是什么？",
                "“孟加拉语”歌曲的平均评分和分辨率是多少？",
                "找出所有至少有一首“英文”歌曲的艺术家的名字和作品数量。"));
        AgentConfig agentConfig = new AgentConfig();

        LLMParserTool llmParserTool = new LLMParserTool();
        llmParserTool.setId("1");
        llmParserTool.setType(AgentToolType.NL2SQL_LLM);
        llmParserTool.setModelIds(Lists.newArrayList(5L, 6L, 7L, 8L));
        agentConfig.getTools().add(llmParserTool);

        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        agentService.createAgent(agent, User.getFakeUser());
    }

}
