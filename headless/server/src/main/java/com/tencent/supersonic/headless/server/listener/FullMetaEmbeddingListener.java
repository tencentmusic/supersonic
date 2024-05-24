package com.tencent.supersonic.headless.server.listener;

import com.tencent.supersonic.headless.core.chat.parser.llm.JavaLLMProxy;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import com.tencent.supersonic.headless.server.schedule.EmbeddingTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
public class FullMetaEmbeddingListener implements CommandLineRunner {
    @Autowired
    private EmbeddingTask embeddingTask;

    @Override
    public void run(String... args) {
        initMetaEmbedding();
    }

    public void initMetaEmbedding() {
        try {
            if (ComponentFactory.getLLMProxy() instanceof JavaLLMProxy) {
                embeddingTask.reloadMetaEmbedding();
            }
        } catch (Exception e) {
            log.error("initMetaEmbedding error", e);
        }
    }
}
