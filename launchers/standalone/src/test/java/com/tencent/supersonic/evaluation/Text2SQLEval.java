package com.tencent.supersonic.evaluation;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.supersonic.chat.BaseTest;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.agent.*;
import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AppModule;
import com.tencent.supersonic.common.util.ChatAppManager;
import com.tencent.supersonic.headless.chat.corrector.LLMSqlCorrector;
import com.tencent.supersonic.util.DataUtils;
import com.tencent.supersonic.util.LLMConfigUtils;
import org.junit.jupiter.api.*;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {"s2.demo.enableLLM = true"})
@Disabled
public class Text2SQLEval extends BaseTest {

    private LLMConfigUtils.LLMType llmType = LLMConfigUtils.LLMType.OLLAMA_LLAMA3;
    private boolean enableLLMCorrection = true;

    @BeforeAll
    public void init() {
        Agent agent = agentService.createAgent(getLLMAgent(), DataUtils.getUser());
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
        assert result.getTextResult().contains("511");
    }

    @Test
    public void test_agg_and_groupby() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("近30日每天的访问次数", agentId);
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryColumns().size() == 2;
        assert result.getQueryResults().size() == 30;
        assert result.getTextResult().contains("date");
    }

    @Test
    public void test_drilldown() throws Exception {
        long start = System.currentTimeMillis();
        QueryResult result = submitNewChat("过去30天每个部门的汇总访问次数", agentId);
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryColumns().size() == 2;
        assert result.getQueryResults().size() == 4;
        assert result.getTextResult().contains("marketing");
        assert result.getTextResult().contains("sales");
        assert result.getTextResult().contains("strategy");
        assert result.getTextResult().contains("HR");
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
        QueryResult result = submitNewChat("近半个月来marketing部门访问量最高的用户是谁", agentId);
        durations.add(System.currentTimeMillis() - start);
        assert result.getQueryResults().size() == 1;
        assert result.getTextResult().contains("dean");
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

    public Agent getLLMAgent() {
        Agent agent = new Agent();
        agent.setName("Agent for Test");
        ToolConfig toolConfig = new ToolConfig();
        toolConfig.getTools().add(getDatasetTool());
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

    private static DatasetTool getDatasetTool() {
        DatasetTool datasetTool = new DatasetTool();
        datasetTool.setType(AgentToolType.DATASET);
        datasetTool.setDataSetIds(Lists.newArrayList(-1L));

        return datasetTool;
    }
}
