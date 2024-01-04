package com.tencent.supersonic;

import com.tencent.supersonic.chat.core.config.OptimizationConfig;
import com.tencent.supersonic.chat.core.parser.JavaLLMProxy;
import com.tencent.supersonic.chat.core.parser.sql.llm.SqlExample;
import com.tencent.supersonic.chat.core.parser.sql.llm.SqlExampleLoader;
import com.tencent.supersonic.chat.core.utils.ComponentFactory;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0)
public class EmbeddingInitListener implements CommandLineRunner {

    @Autowired
    private SqlExampleLoader sqlExampleLoader;
    @Autowired
    private OptimizationConfig optimizationConfig;

    @Override
    public void run(String... args) {
        initSqlExamples();
    }

    public void initSqlExamples() {
        try {
            if (ComponentFactory.getLLMProxy() instanceof JavaLLMProxy) {
                List<SqlExample> sqlExamples = sqlExampleLoader.getSqlExamples();
                String collectionName = optimizationConfig.getText2sqlCollectionName();
                sqlExampleLoader.addEmbeddingStore(sqlExamples, collectionName);
            }
        } catch (Exception e) {
            log.error("initSqlExamples error", e);
        }
    }
}
