package com.tencent.supersonic.integration.plugin;

import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.parser.embedding.EmbeddingConfig;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.util.DataUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;

public class PluginRecognizeTest extends BasePluginTest{

    @MockBean
    private EmbeddingConfig embeddingConfig;

    @MockBean
    protected PluginManager pluginManager;

    @Autowired
    @Qualifier("chatQueryService")
    private QueryService queryService;

    @Test
    public void webPageRecognize() throws Exception {
        PluginMockConfiguration.mockEmbeddingRecognize(pluginManager, "最近的访问情况怎么样","1");
        PluginMockConfiguration.mockEmbeddingUrl(embeddingConfig);
        QueryReq queryContextReq = DataUtils.getQueryContextReq(1000, "alice最近的访问情况怎么样");
        QueryResult queryResult = queryService.executeQuery(queryContextReq);
        assertPluginRecognizeResult(queryResult);
    }

    @Test
    public void webPageRecognizeWithQueryFilter() throws Exception {
        PluginMockConfiguration.mockEmbeddingRecognize(pluginManager, "在超音数最近的情况怎么样","1");
        PluginMockConfiguration.mockEmbeddingUrl(embeddingConfig);
        QueryReq queryRequest = DataUtils.getQueryContextReq(1000, "在超音数最近的情况怎么样");
        QueryFilters queryFilters = new QueryFilters();
        QueryFilter queryFilter = new QueryFilter();
        queryFilter.setElementID(2L);
        queryFilter.setValue("alice");
        queryRequest.setDomainId(1L);
        queryFilters.getFilters().add(queryFilter);
        queryRequest.setQueryFilters(queryFilters);
        QueryResult queryResult = queryService.executeQuery(queryRequest);
        assertPluginRecognizeResult(queryResult);
    }

}
