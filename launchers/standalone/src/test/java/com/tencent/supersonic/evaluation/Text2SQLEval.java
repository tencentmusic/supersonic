package com.tencent.supersonic.evaluation;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.supersonic.chat.BaseTest;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.agent.AgentToolType;
import com.tencent.supersonic.chat.server.agent.DatasetTool;
import com.tencent.supersonic.chat.server.agent.ToolConfig;
import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.demo.S2CompanyDemo;
import com.tencent.supersonic.demo.S2VisitsDemo;
import com.tencent.supersonic.headless.chat.corrector.LLMSqlCorrector;
import com.tencent.supersonic.util.DataUtils;
import com.tencent.supersonic.util.LLMConfigUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {"s2.demo.enableLLM = true"})
@Disabled
public class Text2SQLEval extends BaseTest {

    private final LLMConfigUtils.LLMType llmType = LLMConfigUtils.LLMType.OLLAMA_LLAMA3;
    private final boolean enableLLMCorrection = true;
    protected final List<Long> dataSetIds = Lists.newArrayList();

    @BeforeAll
    public void init() {
        Agent productAgent = getAgentByName(S2VisitsDemo.AGENT_NAME);
        if (Objects.nonNull(productAgent)) {
            dataSetIds.addAll(productAgent.getDataSetIds());
        }
        Agent companyAgent = getAgentByName(S2CompanyDemo.AGENT_NAME);
        if (Objects.nonNull(companyAgent)) {
            dataSetIds.addAll(companyAgent.getDataSetIds());
        }
        agent = agentService.createAgent(getLLMAgent(), DataUtils.getUser());
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
        QueryResult result = submitNewChat("近30天总访问次数", agent.getId());
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryColumns().size() == 1;
        assert result.getTextResult().contains("511");
    }

    @Test
    public void test_agg_and_groupby() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("近30日每天的访问次数", agent.getId());
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryColumns().size() == 2;
        assert result.getQueryResults().size() == 30;
        assert result.getTextResult().contains("date") || result.getTextResult().contains("日期");
    }

    @Test
    public void test_drilldown() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("过去30天每个部门的汇总访问次数", agent.getId());
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryColumns().size() == 2;
        assert result.getQueryResults().size() == 4;
        assert result.getTextResult().contains("marketing");
        assert result.getTextResult().contains("sales");
        assert result.getTextResult().contains("strategy");
        assert result.getTextResult().contains("HR");
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void test_drilldown_and_topN() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("过去30天访问次数最高的部门top3", agent.getId());
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryResults().size() == 3;
        assert result.getTextResult().contains("marketing");
        assert result.getTextResult().contains("sales");
        assert result.getTextResult().contains("strategy");
    }

    @Test
    @SetSystemProperty(key = "s2.test", value = "true")
    public void test_filter_and_top() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("近半个月来marketing部门访问量最高的用户是谁", agent.getId());
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryResults().size() == 1;
        assert result.getTextResult().contains("dean");
    }

    @Test
    public void test_filter() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("近一个月sales部门总访问次数超过10次的用户有哪些", agent.getId());
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryResults().size() == 2;
        assert result.getTextResult().contains("alice");
        assert result.getTextResult().contains("tom");
    }

    @Test
    public void test_filter_compare() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("alice和lucy过去半个月谁的总停留时长更多", agent.getId());
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryResults().size() >= 1;
        assert result.getTextResult().contains("alice");
    }

    @Test
    public void test_term() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("过去半个月每个核心用户的总停留时长", agent.getId());
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryResults().size() == 2;
        assert result.getTextResult().contains("tom");
        assert result.getTextResult().contains("lucy");
    }

    @Test
    public void test_second_calculation() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("近1个月总访问次数超过100次的部门有几个", agent.getId());
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryColumns().size() == 1;
        assert result.getTextResult().contains("3");
    }

    @Test
    public void test_detail_query() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("特斯拉旗下有哪些品牌", agent.getId());
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryColumns().size() >= 1;
        assert result.getTextResult().contains("Model Y");
        assert result.getTextResult().contains("Model 3");
    }

    public Agent getLLMAgent() {
        Agent agent = new Agent();
        agent.setName("Agent for Test");
        ToolConfig toolConfig = new ToolConfig();

        DatasetTool datasetTool = new DatasetTool();
        datasetTool.setType(AgentToolType.DATASET);
        datasetTool.setDataSetIds(dataSetIds);
        toolConfig.getTools().add(datasetTool);

        agent.setToolConfig(JSONObject.toJSONString(toolConfig));
        // create chat model for this evaluation
        ChatModel chatModel = new ChatModel();
        chatModel.setName("Text2SQL LLM");
        chatModel.setConfig(LLMConfigUtils.getLLMConfig(llmType));
        chatModel = chatModelService.createChatModel(chatModel, User.getDefaultUser());
        Integer chatModelId = chatModel.getId();
        // configure chat apps
        Map<String, ChatApp> chatAppConfig =
                Maps.newHashMap(ChatAppManager.getAllApps(AppModule.CHAT));
        chatAppConfig.values().forEach(app -> app.setChatModelId(chatModelId));
        chatAppConfig.get(LLMSqlCorrector.APP_KEY).setEnable(enableLLMCorrection);
        agent.setChatAppConfig(chatAppConfig);
        return agent;
    }

}
