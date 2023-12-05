package com.tencent.supersonic.integration;


import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.plugin.PluginManager;
import com.tencent.supersonic.chat.service.AgentService;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.util.embedding.EmbeddingUtils;
import com.tencent.supersonic.common.util.embedding.Retrieval;
import com.tencent.supersonic.common.util.embedding.RetrieveQueryResult;
import com.tencent.supersonic.util.DataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

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

    public static void mockAgent(AgentService agentService) {
        when(agentService.getAgent(1)).thenReturn(DataUtils.getAgent());
    }

    public static void embeddingUtils(EmbeddingUtils embeddingUtils, ResponseEntity<String> responseEntity) {
        when(embeddingUtils.doRequest(anyObject(), anyObject(), anyObject())).thenReturn(responseEntity);
    }
}
