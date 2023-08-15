package com.tencent.supersonic.integration.plugin;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.parser.embedding.EmbeddingConfig;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.query.ContentInterpret.LLmAnswerResp;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.util.DataUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

public class PluginRecognizeTest extends BasePluginTest {

    @MockBean
    private EmbeddingConfig embeddingConfig;

    @MockBean
    protected PluginManager pluginManager;

    @Autowired
    @Qualifier("chatQueryService")
    private QueryService queryService;

    @Test
    public void webPageRecognize() throws Exception {
        PluginMockConfiguration.mockEmbeddingRecognize(pluginManager, "alice最近的访问情况怎么样", "1");
        PluginMockConfiguration.mockEmbeddingUrl(embeddingConfig);
        QueryReq queryContextReq = DataUtils.getQueryContextReq(1000, "alice最近的访问情况怎么样");
        QueryResult queryResult = queryService.executeQuery(queryContextReq);
        assertPluginRecognizeResult(queryResult);
    }

    @Test
    public void webPageRecognizeWithQueryFilter() throws Exception {
        PluginMockConfiguration.mockEmbeddingRecognize(pluginManager, "在超音数最近的情况怎么样", "1");
        PluginMockConfiguration.mockEmbeddingUrl(embeddingConfig);
        QueryReq queryRequest = DataUtils.getQueryContextReq(1000, "在超音数最近的情况怎么样");
        QueryFilters queryFilters = new QueryFilters();
        QueryFilter queryFilter = new QueryFilter();
        queryFilter.setElementID(2L);
        queryFilter.setValue("alice");
        queryRequest.setModelId(1L);
        queryFilters.getFilters().add(queryFilter);
        queryRequest.setQueryFilters(queryFilters);
        QueryResult queryResult = queryService.executeQuery(queryRequest);
        assertPluginRecognizeResult(queryResult);
    }

    @Test
    public void contentInterpretRecognize() throws Exception {
        PluginMockConfiguration.mockEmbeddingRecognize(pluginManager, "超音数最近访问情况怎么样", "3");
        PluginMockConfiguration.mockEmbeddingUrl(embeddingConfig);
        LLmAnswerResp lLmAnswerResp = new LLmAnswerResp();
        lLmAnswerResp.setAssistant_message("超音数最近访问情况不错");
        PluginMockConfiguration.mockPluginManagerDoRequest(pluginManager, "answer_with_plugin_call",
                ResponseEntity.ok(JSONObject.toJSONString(lLmAnswerResp)));
        QueryReq queryRequest = DataUtils.getQueryContextReq(1000, "超音数最近访问情况怎么样");
        QueryResult queryResult = queryService.executeQuery(queryRequest);
        Assert.assertEquals(queryResult.getResponse(), lLmAnswerResp.getAssistant_message());
        System.out.println();
    }

}
