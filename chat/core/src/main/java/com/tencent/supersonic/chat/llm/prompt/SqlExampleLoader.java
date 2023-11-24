package com.tencent.supersonic.chat.llm.prompt;


import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.supersonic.chat.llm.vectordb.EmbeddingStoreOperator;
import com.tencent.supersonic.common.util.JsonUtil;
import dev.langchain4j.data.segment.TextSegment;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SqlExampleLoader {

    private static final String EXAMPLE_JSON_FILE = "example.json";
    @Autowired
    private EmbeddingStoreOperator embeddingStoreOperator;
    private TypeReference<List<SqlExample>> valueTypeRef = new TypeReference<List<SqlExample>>() {
    };

    public List<SqlExample> getSqlExamples() throws IOException {
        ClassPathResource resource = new ClassPathResource(EXAMPLE_JSON_FILE);
        InputStream inputStream = resource.getInputStream();
        return JsonUtil.INSTANCE.getObjectMapper().readValue(inputStream, valueTypeRef);
    }

    public void addEmbeddingStore(List<SqlExample> sqlExamples, String collectionName) {
        embeddingStoreOperator.addAll(sqlExamples, collectionName);
    }

    public List<Map<String, String>> retrieverSqlExamples(String queryText, String collectionName, int maxResults) {
        List<TextSegment> textSegments = embeddingStoreOperator.retriever(queryText, collectionName, maxResults);

        List<Map<String, String>> result = new ArrayList<>();
        for (TextSegment textSegment : textSegments) {
            if (Objects.nonNull(textSegment.metadata())) {
                result.add(textSegment.metadata().asMap());
            }
        }
        return result;
    }
}
