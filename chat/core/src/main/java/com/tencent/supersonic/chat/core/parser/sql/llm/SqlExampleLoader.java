package com.tencent.supersonic.chat.core.parser.sql.llm;


import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.supersonic.common.util.ComponentFactory;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.embedding.EmbeddingQuery;
import com.tencent.supersonic.common.util.embedding.Retrieval;
import com.tencent.supersonic.common.util.embedding.RetrieveQuery;
import com.tencent.supersonic.common.util.embedding.RetrieveQueryResult;
import com.tencent.supersonic.common.util.embedding.S2EmbeddingStore;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SqlExampleLoader {

    private static final String EXAMPLE_JSON_FILE = "s2ql_examplar.json";

    private S2EmbeddingStore s2EmbeddingStore = ComponentFactory.getS2EmbeddingStore();
    private TypeReference<List<SqlExample>> valueTypeRef = new TypeReference<List<SqlExample>>() {
    };

    public List<SqlExample> getSqlExamples() throws IOException {
        ClassPathResource resource = new ClassPathResource(EXAMPLE_JSON_FILE);
        InputStream inputStream = resource.getInputStream();
        return JsonUtil.INSTANCE.getObjectMapper().readValue(inputStream, valueTypeRef);
    }

    public void addEmbeddingStore(List<SqlExample> sqlExamples, String collectionName) {
        List<EmbeddingQuery> queries = new ArrayList<>();
        for (int i = 0; i < sqlExamples.size(); i++) {
            SqlExample sqlExample = sqlExamples.get(i);
            String question = sqlExample.getQuestion();
            Map<String, Object> metaDataMap = JsonUtil.toMap(JsonUtil.toString(sqlExample), String.class, Object.class);
            EmbeddingQuery embeddingQuery = new EmbeddingQuery();
            embeddingQuery.setQueryId(String.valueOf(i));
            embeddingQuery.setQuery(question);
            embeddingQuery.setMetadata(metaDataMap);
            queries.add(embeddingQuery);
        }
        s2EmbeddingStore.addQuery(collectionName, queries);
    }

    public List<Map<String, String>> retrieverSqlExamples(String queryText, String collectionName, int maxResults) {

        RetrieveQuery retrieveQuery = RetrieveQuery.builder().queryTextsList(Collections.singletonList(queryText))
                .queryEmbeddings(null).build();

        List<RetrieveQueryResult> resultList = s2EmbeddingStore.retrieveQuery(collectionName, retrieveQuery,
                maxResults);
        List<Map<String, String>> result = new ArrayList<>();
        if (CollectionUtils.isEmpty(resultList)) {
            return result;
        }
        for (Retrieval retrieval : resultList.get(0).getRetrieval()) {
            if (Objects.nonNull(retrieval.getMetadata()) && !retrieval.getMetadata().isEmpty()) {
                Map<String, String> convertedMap = retrieval.getMetadata().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
                result.add(convertedMap);
            }
        }
        return result;
    }
}
