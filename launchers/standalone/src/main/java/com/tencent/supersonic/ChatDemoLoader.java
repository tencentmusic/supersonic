package com.tencent.supersonic;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentConfig;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.LLMParserTool;
import com.tencent.supersonic.chat.server.agent.RuleParserTool;
import com.tencent.supersonic.chat.server.plugin.Plugin;
import com.tencent.supersonic.chat.server.plugin.PluginParseConfig;
import com.tencent.supersonic.chat.server.plugin.build.ParamOption;
import com.tencent.supersonic.chat.server.plugin.build.WebBase;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import com.tencent.supersonic.chat.server.service.ChatService;
import com.tencent.supersonic.chat.server.service.PluginService;
import com.tencent.supersonic.common.pojo.SysParameter;
import com.tencent.supersonic.common.service.SysParameterService;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@Order(3)
public class ChatDemoLoader implements CommandLineRunner {

    private User user = User.getFakeUser();
    @Autowired
    private ChatService chatService;
    @Autowired
    private ChatManageService chatManageService;
    @Autowired
    private PluginService pluginService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private SysParameterService sysParameterService;

    @Value("${demo.enabled:false}")
    private boolean demoEnabled;

    @Value("${demo.nl2SqlLlm.enabled:true}")
    private boolean demoEnabledNl2SqlLlm;

    @Override
    public void run(String... args) throws Exception {
        if (!checkEnable()) {
            log.info("skip load chat demo");
            return;
        }
        doRun();
    }

    public void doRun() {
        try {
            addSysParameter();
            addPlugin_1();
            addAgent1();
            addAgent2();
            addAgent3();
            //addAgent4();
            addSampleChats();
            addSampleChats2();
            updateQueryScore(1);
            updateQueryScore(4);
        } catch (Exception e) {
            log.error("Failed to add sample chats", e);
        }
    }

    private void parseAndExecute(int chatId, String queryText) throws Exception {
        ChatParseReq chatParseReq = new ChatParseReq();
        chatParseReq.setQueryText(queryText);
        chatParseReq.setChatId(chatId);
        chatParseReq.setAgentId(1);
        chatParseReq.setUser(User.getFakeUser());
        ParseResp parseResp = chatService.performParsing(chatParseReq);
        if (CollectionUtils.isEmpty(parseResp.getSelectedParses())) {
            log.info("parseResp.getSelectedParses() is empty");
            return;
        }
        ChatExecuteReq executeReq = new ChatExecuteReq();
        executeReq.setQueryId(parseResp.getQueryId());
        executeReq.setParseId(parseResp.getSelectedParses().get(0).getId());
        executeReq.setQueryText(queryText);
        executeReq.setChatId(parseResp.getChatId());
        executeReq.setUser(User.getFakeUser());
        executeReq.setSaveAnswer(true);
        chatService.performExecution(executeReq);
    }

    public void addSampleChats() throws Exception {
        chatManageService.addChat(user, "样例对话1", 1);

        parseAndExecute(1, "超音数 访问次数");
        parseAndExecute(1, "按部门统计");
        parseAndExecute(1, "查询近30天");
    }

    public void addSampleChats2() throws Exception {
        chatManageService.addChat(user, "样例对话2", 1);

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

    private void addPlugin_1() {
        Plugin plugin1 = new Plugin();
        plugin1.setType("WEB_PAGE");
        plugin1.setDataSetList(Arrays.asList(1L));
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
        ruleQueryTool.setDataSetIds(Lists.newArrayList(1L));
        agentConfig.getTools().add(ruleQueryTool);
        if (demoEnabledNl2SqlLlm) {
            LLMParserTool llmParserTool = new LLMParserTool();
            llmParserTool.setId("1");
            llmParserTool.setType(AgentToolType.NL2SQL_LLM);
            llmParserTool.setDataSetIds(Lists.newArrayList(-1L));
            agentConfig.getTools().add(llmParserTool);
        }
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
        ruleQueryTool.setDataSetIds(Lists.newArrayList(2L));
        agentConfig.getTools().add(ruleQueryTool);

        if (demoEnabledNl2SqlLlm) {
            LLMParserTool llmParserTool = new LLMParserTool();
            llmParserTool.setId("1");
            llmParserTool.setType(AgentToolType.NL2SQL_LLM);
            llmParserTool.setDataSetIds(Lists.newArrayList(-1L));
            agentConfig.getTools().add(llmParserTool);
        }
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
        if (demoEnabledNl2SqlLlm) {
            LLMParserTool llmParserTool = new LLMParserTool();
            llmParserTool.setId("1");
            llmParserTool.setType(AgentToolType.NL2SQL_LLM);
            llmParserTool.setDataSetIds(Lists.newArrayList(3L));
            agentConfig.getTools().add(llmParserTool);
        }

        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        agentService.createAgent(agent, User.getFakeUser());
    }

    private void addAgent4() {
        Agent agent = new Agent();
        agent.setId(4);
        agent.setName("DuSQL 互联网企业");
        agent.setDescription("DuSQL");
        agent.setStatus(1);
        agent.setEnableSearch(1);
        agent.setExamples(Lists.newArrayList());
        AgentConfig agentConfig = new AgentConfig();

        if (demoEnabledNl2SqlLlm) {
            LLMParserTool llmParserTool = new LLMParserTool();
            llmParserTool.setId("1");
            llmParserTool.setType(AgentToolType.NL2SQL_LLM);
            llmParserTool.setDataSetIds(Lists.newArrayList(4L));
            agentConfig.getTools().add(llmParserTool);
        }

        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        log.info("agent:{}", JsonUtil.toString(agent));
        agentService.createAgent(agent, User.getFakeUser());
    }

    private void updateQueryScore(Integer queryId) {
        chatManageService.updateFeedback(queryId, 5, "");
    }

    private boolean checkEnable() {
        if (!demoEnabled) {
            return false;
        }
        return HeadlessDemoLoader.isLoad();
    }

}
