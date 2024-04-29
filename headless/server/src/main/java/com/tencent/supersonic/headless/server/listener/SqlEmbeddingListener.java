package com.tencent.supersonic.headless.server.listener;

import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.headless.core.chat.parser.JavaLLMProxy;
import com.tencent.supersonic.headless.core.chat.parser.llm.SqlExamplarLoader;
import com.tencent.supersonic.headless.core.chat.parser.llm.SqlExample;
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
    private SqlExamplarLoader sqlExamplarLoader;
    @Autowired
    private EmbeddingConfig embeddingConfig;

    @Override
    public void run(String... args) {
        initSqlExamples();
    }

    public void initSqlExamples() {
        try {
            if (ComponentFactory.getLLMProxy() instanceof JavaLLMProxy) {
                List<SqlExample> sqlExamples = sqlExamplarLoader.getSqlExamples();
                String collectionName = embeddingConfig.getText2sqlCollectionName();
                sqlExamplarLoader.addEmbeddingStore(sqlExamples, collectionName);
            }
        } catch (Exception e) {
            log.error("initSqlExamples error", e);
        }
    }
}
