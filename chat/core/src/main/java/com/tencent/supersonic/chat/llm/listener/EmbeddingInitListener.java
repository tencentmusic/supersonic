package com.tencent.supersonic.chat.llm.listener;

import com.tencent.supersonic.chat.config.OptimizationConfig;
import com.tencent.supersonic.chat.llm.EmbedLLMInterpreter;
import com.tencent.supersonic.chat.llm.LLMInterpreter;
import com.tencent.supersonic.chat.llm.prompt.SqlExample;
import com.tencent.supersonic.chat.llm.prompt.SqlExampleLoader;
import com.tencent.supersonic.chat.utils.ComponentFactory;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(4)
public class EmbeddingInitListener implements CommandLineRunner {

    protected LLMInterpreter llmInterpreter = ComponentFactory.getLLMInterpreter();
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
            if (llmInterpreter instanceof EmbedLLMInterpreter) {
                List<SqlExample> sqlExamples = sqlExampleLoader.getSqlExamples();
                String collectionName = optimizationConfig.getText2sqlCollectionName();
                sqlExampleLoader.addEmbeddingStore(sqlExamples, collectionName);
            }
        } catch (Exception e) {
            log.error("initSqlExamples error", e);
        }
    }
}
