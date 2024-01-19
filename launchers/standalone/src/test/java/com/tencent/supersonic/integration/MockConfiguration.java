package com.tencent.supersonic.integration;


import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.core.plugin.PluginManager;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.util.embedding.Retrieval;
import com.tencent.supersonic.common.util.embedding.RetrieveQueryResult;
import com.tencent.supersonic.util.DataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MockConfiguration {

    public static void mockEmbeddingRecognize(PluginManager pluginManager, String text, String id) {
        RetrieveQueryResult embeddingResp = new RetrieveQueryResult();
        Retrieval embeddingRetrieval = new Retrieval();
        embeddingRetrieval.setId(id);
        embeddingRetrieval.setDistance(0.15);
        embeddingResp.setQuery(text);
        embeddingResp.setRetrieval(Lists.newArrayList(embeddingRetrieval));
        when(pluginManager.recognize(text)).thenReturn(embeddingResp);
    }

    public static void mockEmbeddingUrl(EmbeddingConfig embeddingConfig) {
        when(embeddingConfig.getUrl()).thenReturn("test");
    }

    public static void mockMetricAgent(AgentService agentService) {
        when(agentService.getAgent(1)).thenReturn(DataUtils.getMetricAgent());
    }

    public static void mockTagAgent(AgentService agentService) {
        when(agentService.getAgent(2)).thenReturn(DataUtils.getTagAgent());
    }

}
