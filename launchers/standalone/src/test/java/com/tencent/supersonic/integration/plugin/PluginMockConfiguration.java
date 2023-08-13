package com.tencent.supersonic.integration.plugin;


import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.parser.embedding.EmbeddingConfig;
import com.tencent.supersonic.chat.parser.embedding.EmbeddingResp;
import com.tencent.supersonic.chat.parser.embedding.RecallRetrieval;
import com.tencent.supersonic.chat.plugin.PluginManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

@Configuration
@Slf4j
public class PluginMockConfiguration {

    public static void mockEmbeddingRecognize(PluginManager pluginManager, String text, String id) {
        EmbeddingResp embeddingResp = new EmbeddingResp();
        RecallRetrieval embeddingRetrieval = new RecallRetrieval();
        embeddingRetrieval.setId(id);
        embeddingRetrieval.setPresetId(id);
        embeddingRetrieval.setDistance("0.15");
        embeddingResp.setQuery(text);
        embeddingResp.setRetrieval(Lists.newArrayList(embeddingRetrieval));
        when(pluginManager.recognize(text)).thenReturn(embeddingResp);
    }

    public static void mockEmbeddingUrl(EmbeddingConfig embeddingConfig) {
        when(embeddingConfig.getUrl()).thenReturn("test");
    }

    public static void mockPluginManagerDoRequest(PluginManager pluginManager, String path,
            ResponseEntity<String> responseEntity) {
        when(pluginManager.doRequest(eq(path), notNull(String.class))).thenReturn(responseEntity);
    }

}
