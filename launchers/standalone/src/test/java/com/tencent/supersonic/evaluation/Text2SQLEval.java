package com.tencent.supersonic.evaluation;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.BaseTest;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentConfig;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.MultiTurnConfig;
import com.tencent.supersonic.chat.server.agent.RuleParserTool;
import com.tencent.supersonic.common.config.LLMConfig;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.util.DataUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
public class Text2SQLEval extends BaseTest {

    private int agentId;

    @BeforeAll
    public void init() {
        agentId = agentService.createAgent(getLLMAgent(false), DataUtils.getUser());
    }

    @Test
    public void test_agg() throws Exception {
        QueryResult result = submitNewChat("近30天访问次数", agentId);
        assert result.getQueryColumns().size() == 1;
        assert result.getQueryColumns().get(0).getName().contains("访问次数");
    }

    @Test
    public void test_agg_and_groupby() throws Exception {
        QueryResult result = submitNewChat("近30日每天的访问次数", agentId);
        assert result.getQueryColumns().size() == 2;
        assert result.getQueryColumns().get(0).getName().equalsIgnoreCase("date");
        assert result.getQueryColumns().get(1).getName().contains("访问次数");
    }

    @Test
    public void test_drilldown() throws Exception {
        QueryResult result = submitNewChat("过去30天每个部门的汇总访问次数", agentId);
        assert result.getQueryColumns().size() == 2;
        assert result.getQueryColumns().get(0).getName().equalsIgnoreCase("部门");
        assert result.getQueryColumns().get(1).getName().contains("访问次数");
        assert result.getQueryResults().size() == 4;
    }

    @Test
    public void test_drilldown_and_topN() throws Exception {
        QueryResult result = submitNewChat("过去30天访问次数最高的部门top2", agentId);
        assert result.getQueryColumns().size() == 2;
        assert result.getQueryColumns().get(0).getName().equalsIgnoreCase("部门");
        assert result.getQueryColumns().get(1).getName().contains("访问次数");
        assert result.getQueryResults().size() == 2;
    }

    @Test
    public void test_filter_and_top() throws Exception {
        QueryResult result = submitNewChat("近半个月sales部门访问量最高的用户是谁", agentId);
        assert result.getQueryColumns().size() == 2;
        assert result.getQueryColumns().get(0).getName().contains("用户");
        assert result.getQueryColumns().get(1).getName().contains("访问次数");
        assert result.getQueryResults().size() == 1;
    }

    @Test
    public void test_filter() throws Exception {
        QueryResult result = submitNewChat("近一个月sales部门总访问次数超过10次的用户有哪些", agentId);
        assert result.getQueryColumns().size() == 2;
        assert result.getQueryColumns().get(0).getName().contains("用户");
        assert result.getQueryColumns().get(1).getName().contains("访问次数");
        assert result.getQueryResults().size() == 2;
    }

    public static Agent getLLMAgent(boolean enableMultiturn) {
        Agent agent = new Agent();
        agent.setName("Agent for Test");
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.getTools().add(getLLMQueryTool());
        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        agent.setLlmConfig(getLLMConfig(LLMType.GPT));
        MultiTurnConfig multiTurnConfig = new MultiTurnConfig();
        multiTurnConfig.setEnableMultiTurn(enableMultiturn);
        agent.setMultiTurnConfig(multiTurnConfig);
        return agent;
    }

    private static RuleParserTool getLLMQueryTool() {
        RuleParserTool ruleQueryTool = new RuleParserTool();
        ruleQueryTool.setType(AgentToolType.NL2SQL_LLM);
        ruleQueryTool.setDataSetIds(Lists.newArrayList(-1L));

        return ruleQueryTool;
    }

    private enum LLMType {
        GPT,
        MOONSHOT,
        DEEPSEEK,
        QWEN,
        GLM
    }

    private static LLMConfig getLLMConfig(LLMType type) {
        String baseUrl;
        String apiKey;
        String modelName;
        double temperature = 0.0;

        switch (type) {
            case GLM:
                baseUrl = "https://open.bigmodel.cn/api/paas/v4/";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "glm-4";
                break;
            case MOONSHOT:
                baseUrl = "https://api.moonshot.cn/v1";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "moonshot-v1-8k";
                break;
            case DEEPSEEK:
                baseUrl = "https://api.deepseek.com";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "deepseek-coder";
                break;
            case QWEN:
                baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "qwen-turbo";
                temperature = 0.01;
                break;
            case GPT:
            default:
                baseUrl = "https://api.openai.com/v1";
                apiKey = "REPLACE_WITH_YOUR_KEY";
                modelName = "gpt-3.5-turbo";
                temperature = 0.0;
        }

        return new LLMConfig("open_ai",
                baseUrl, apiKey, modelName, temperature);
    }

}
