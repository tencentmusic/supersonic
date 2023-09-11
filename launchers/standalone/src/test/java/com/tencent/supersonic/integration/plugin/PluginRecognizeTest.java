package com.tencent.supersonic.integration.plugin;

import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.ParseResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.parser.plugin.embedding.EmbeddingConfig;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.integration.MockConfiguration;
import com.tencent.supersonic.util.DataUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;

public class PluginRecognizeTest extends BasePluginTest {

    @MockBean
    protected PluginManager pluginManager;

    @MockBean
    private EmbeddingConfig embeddingConfig;

    @MockBean
    private AgentService agentService;

    @Autowired
    @Qualifier("chatQueryService")
    private QueryService queryService;

    @Test
    public void webPageRecognize() throws Exception {
        MockConfiguration.mockEmbeddingRecognize(pluginManager, "alice最近的访问情况怎么样", "1");
        MockConfiguration.mockEmbeddingUrl(embeddingConfig);
        QueryReq queryContextReq = DataUtils.getQueryReqWithAgent(1000, "alice最近的访问情况怎么样", 1);
        QueryResult queryResult = queryService.executeQuery(queryContextReq);
        assertPluginRecognizeResult(queryResult);
    }

    @Test
    public void webPageRecognizeWithQueryFilter() throws Exception {
        MockConfiguration.mockEmbeddingRecognize(pluginManager, "在超音数最近的情况怎么样", "1");
        MockConfiguration.mockEmbeddingUrl(embeddingConfig);
        QueryReq queryRequest = DataUtils.getQueryReqWithAgent(1000, "在超音数最近的情况怎么样", 1);
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
    public void pluginRecognizeWithAgent() {
        MockConfiguration.mockEmbeddingRecognize(pluginManager, "alice最近的访问情况怎么样", "1");
        MockConfiguration.mockEmbeddingUrl(embeddingConfig);
        MockConfiguration.mockAgent(agentService);
        QueryReq queryContextReq = DataUtils.getQueryReqWithAgent(1000, "alice最近的访问情况怎么样",
                DataUtils.getAgent().getId());
        ParseResp parseResp = queryService.performParsing(queryContextReq);
        Assert.assertTrue(parseResp.getSelectedParses() != null
                && parseResp.getSelectedParses().size() > 0);
    }

}
