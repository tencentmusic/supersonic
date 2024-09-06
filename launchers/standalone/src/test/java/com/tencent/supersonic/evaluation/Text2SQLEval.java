package com.tencent.supersonic.evaluation;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.BaseTest;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentConfig;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.MultiTurnConfig;
import com.tencent.supersonic.chat.server.agent.RuleParserTool;
import com.tencent.supersonic.util.DataUtils;
import com.tencent.supersonic.util.LLMConfigUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
public class Text2SQLEval extends BaseTest {

    private int agentId;

    @BeforeAll
    public void init() {
        Agent agent = agentService.createAgent(getLLMAgent(false), DataUtils.getUser());
        agentId = agent.getId();
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

    @Test
    public void test_filter_compare() throws Exception {
        QueryResult result = submitNewChat("alice和lucy过去半个月哪一位的总停留时长更高", agentId);
        assert result.getQueryColumns().size() == 2;
        assert result.getQueryColumns().get(0).getName().contains("用户");
        assert result.getQueryColumns().get(1).getName().contains("停留时长");
        assert result.getQueryResults().size() >= 1;
    }

    @Test
    public void test_term() throws Exception {
        QueryResult result = submitNewChat("过去半个月核心用户的总停留时长", agentId);
        assert result.getQueryColumns().size() >= 1;
        assert result.getQueryColumns().stream()
                        .filter(c -> c.getName().contains("停留时长"))
                        .collect(Collectors.toList())
                        .size()
                == 1;
        assert result.getQueryResults().size() >= 1;
    }

    public Agent getLLMAgent(boolean enableMultiturn) {
        Agent agent = new Agent();
        agent.setName("Agent for Test");
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.getTools().add(getLLMQueryTool());
        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        agent.setModelConfig(LLMConfigUtils.getLLMConfig(LLMConfigUtils.LLMType.GPT));
        MultiTurnConfig multiTurnConfig = new MultiTurnConfig();
        multiTurnConfig.setEnableMultiTurn(enableMultiturn);
        agent.setMultiTurnConfig(multiTurnConfig);
        return agent;
    }

    private RuleParserTool getLLMQueryTool() {
        RuleParserTool ruleQueryTool = new RuleParserTool();
        ruleQueryTool.setType(AgentToolType.NL2SQL_LLM);
        ruleQueryTool.setDataSetIds(Lists.newArrayList(-1L));

        return ruleQueryTool;
    }
}
