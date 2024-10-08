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
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
public class Text2SQLEval extends BaseTest {

    private int agentId;
    private List<Long> durations = Lists.newArrayList();

    @BeforeAll
    public void init() {
        Agent agent = agentService.createAgent(getLLMAgent(false), DataUtils.getUser());
        agentId = agent.getId();
    }

    @AfterAll
    public void summarize() {
        long total_duration = 0L;
        for (Long duration : durations) {
            total_duration += duration;
        }
        System.out.println(String.format("Avg Duration: %d seconds",
                total_duration / 1000 / durations.size()));
    }

    @Test
    public void test_agg() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("近30天总访问次数", agentId);
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryColumns().size() == 1;
        assert result.getQueryColumns().get(0).getName().contains("访问次数");
    }

    @Test
    public void test_agg_and_groupby() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("近30日每天的访问次数", agentId);
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryColumns().size() == 2;
        assert result.getQueryColumns().get(0).getName().equalsIgnoreCase("date");
        assert result.getQueryColumns().get(1).getName().contains("访问次数");
    }

    @Test
    public void test_drilldown() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("过去30天每个部门的汇总访问次数", agentId);
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryColumns().size() == 2;
        assert result.getQueryColumns().get(0).getName().equalsIgnoreCase("部门");
        assert result.getQueryColumns().get(1).getName().contains("访问次数");
        assert result.getQueryResults().size() == 4;
    }

    @Test
    public void test_drilldown_and_topN() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("过去30天访问次数最高的部门top3", agentId);
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryResults().size() == 3;
        assert result.getTextResult().contains("marketing");
        assert result.getTextResult().contains("sales");
        assert result.getTextResult().contains("strategy");
    }

    @Test
    public void test_filter_and_top() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("近半个月来sales部门访问量最高的用户是谁", agentId);
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryResults().size() == 1;
        assert result.getTextResult().contains("tom");
    }

    @Test
    public void test_filter() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("近一个月sales部门总访问次数超过10次的用户有哪些", agentId);
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryResults().size() == 2;
        assert result.getTextResult().contains("alice");
        assert result.getTextResult().contains("tom");
    }

    @Test
    public void test_filter_compare() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("alice和lucy过去半个月谁的总停留时长更多", agentId);
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryResults().size() >= 1;
        assert result.getTextResult().contains("alice");
    }

    @Test
    public void test_term() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("过去半个月每个核心用户的总停留时长", agentId);
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryResults().size() == 2;
        assert result.getTextResult().contains("tom");
        assert result.getTextResult().contains("lucy");
    }

    @Test
    public void test_second_calculation() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("近1个月总访问次数超过100次的部门有几个", agentId);
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryColumns().size() == 1;
        assert result.getTextResult().contains("3");
    }

    public Agent getLLMAgent(boolean enableMultiturn) {
        Agent agent = new Agent();
        agent.setName("Agent for Test");
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.getTools().add(getLLMQueryTool());
        agent.setAgentConfig(JSONObject.toJSONString(agentConfig));
        agent.setModelConfig(LLMConfigUtils.getLLMConfig(LLMConfigUtils.LLMType.OLLAMA_LLAMA3));
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
