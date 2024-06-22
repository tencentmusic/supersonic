package com.tencent.supersonic.headless.chat.parser.llm;


import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import dev.langchain4j.store.embedding.ComponentFactory;
import com.tencent.supersonic.common.util.JsonUtil;
import dev.langchain4j.store.embedding.EmbeddingQuery;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import dev.langchain4j.store.embedding.S2EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ExemplarManager {

    private static final String EXAMPLE_JSON_FILE = "s2ql_exemplar.json";

    private S2EmbeddingStore s2EmbeddingStore = ComponentFactory.getS2EmbeddingStore();
    private TypeReference<List<Exemplar>> valueTypeRef = new TypeReference<List<Exemplar>>() {
    };

    @Autowired
    private EmbeddingConfig embeddingConfig;

    public List<Exemplar> getExemplars() throws IOException {
        ClassPathResource resource = new ClassPathResource(EXAMPLE_JSON_FILE);
        InputStream inputStream = resource.getInputStream();
        return JsonUtil.INSTANCE.getObjectMapper().readValue(inputStream, valueTypeRef);
    }

    public void addExemplars(List<Exemplar> exemplars, String collectionName) {
        List<EmbeddingQuery> queries = new ArrayList<>();
        for (int i = 0; i < exemplars.size(); i++) {
            Exemplar exemplar = exemplars.get(i);
            String question = exemplar.getQuestion();
            Map<String, Object> metaDataMap = JsonUtil.toMap(JsonUtil.toString(exemplar), String.class, Object.class);
            EmbeddingQuery embeddingQuery = new EmbeddingQuery();
            embeddingQuery.setQueryId(String.valueOf(i));
            embeddingQuery.setQuery(question);
            embeddingQuery.setMetadata(metaDataMap);
            queries.add(embeddingQuery);
        }
        s2EmbeddingStore.addQuery(collectionName, queries);
    }

    public List<Map<String, String>> recallExemplars(String queryText, int maxResults) {
        String collectionName = embeddingConfig.getText2sqlCollectionName();
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
