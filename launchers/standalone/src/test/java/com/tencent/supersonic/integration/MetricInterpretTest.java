package com.tencent.supersonic.integration;

import com.tencent.supersonic.StandaloneLauncher;
import com.tencent.supersonic.chat.core.query.llm.analytics.LLMAnswerResp;
import com.tencent.supersonic.chat.server.service.AgentService;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StandaloneLauncher.class)
@ActiveProfiles("local")
public class MetricInterpretTest {

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
