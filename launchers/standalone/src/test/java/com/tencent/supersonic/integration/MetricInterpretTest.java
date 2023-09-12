package com.tencent.supersonic.integration;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.StandaloneLauncher;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.parser.plugin.embedding.EmbeddingConfig;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.query.llm.interpret.LLmAnswerResp;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.util.DataUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StandaloneLauncher.class)
@ActiveProfiles("local")
public class MetricInterpretTest {

    @MockBean
    private AgentService agentService;

    @MockBean
    private PluginManager pluginManager;

    @MockBean
    private EmbeddingConfig embeddingConfig;

    @Autowired
    @Qualifier("chatQueryService")
    private QueryService queryService;

    @Test
    public void testMetricInterpret() throws Exception {
        MockConfiguration.mockAgent(agentService);
        MockConfiguration.mockEmbeddingUrl(embeddingConfig);
        LLmAnswerResp lLmAnswerResp = new LLmAnswerResp();
        lLmAnswerResp.setAssistantMessage("alice最近在超音数的访问情况有增多");
        MockConfiguration.mockPluginManagerDoRequest(pluginManager, "answer_with_plugin_call",
                ResponseEntity.ok(JSONObject.toJSONString(lLmAnswerResp)));
        QueryReq queryReq = DataUtils.getQueryReqWithAgent(1000, "能不能帮我解读分析下最近alice在超音数的访问情况",
                DataUtils.getAgent().getId());
        QueryResult queryResult = queryService.executeQuery(queryReq);
        Assert.assertEquals(queryResult.getQueryResults().get(0).get("answer"), lLmAnswerResp.getAssistantMessage());
    }

}
