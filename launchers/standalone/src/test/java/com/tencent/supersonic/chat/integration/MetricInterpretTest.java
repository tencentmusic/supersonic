package com.tencent.supersonic.chat.integration;

import com.tencent.supersonic.BaseApplication;
import com.tencent.supersonic.chat.core.query.llm.analytics.LLMAnswerResp;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

public class MetricInterpretTest extends BaseApplication {

    @MockBean
    private AgentService agentService;
    @MockBean
    private EmbeddingConfig embeddingConfig;

    @Test
    public void testMetricInterpret() throws Exception {
        MockConfiguration.mockMetricAgent(agentService);
        MockConfiguration.mockEmbeddingUrl(embeddingConfig);

        LLMAnswerResp lLmAnswerResp = new LLMAnswerResp();
        lLmAnswerResp.setAssistantMessage("alice最近在超音数的访问情况有增多");

    }

}
