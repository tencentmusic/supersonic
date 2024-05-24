package com.tencent.supersonic.headless.server.listener;

import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.headless.core.chat.parser.llm.JavaLLMProxy;
import com.tencent.supersonic.headless.core.chat.parser.llm.ExemplarManager;
import com.tencent.supersonic.headless.core.chat.parser.llm.Exemplar;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(0)
public class SqlEmbeddingListener implements CommandLineRunner {

    @Autowired
    private ExemplarManager exemplarManager;
    @Autowired
    private EmbeddingConfig embeddingConfig;

    @Override
    public void run(String... args) {
        initSqlExamples();
    }

    public void initSqlExamples() {
        try {
            if (ComponentFactory.getLLMProxy() instanceof JavaLLMProxy) {
                List<Exemplar> exemplars = exemplarManager.getExemplars();
                String collectionName = embeddingConfig.getText2sqlCollectionName();
                exemplarManager.addExemplars(exemplars, collectionName);
            }
        } catch (Exception e) {
            log.error("initSqlExamples error", e);
        }
    }
}
