package com.tencent.supersonic.headless.chat.parser.llm;


import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.service.EmbeddingService;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.chat.utils.ComponentFactory;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import dev.langchain4j.store.embedding.TextSegmentConvert;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
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
@Order(0)
public class ExemplarManager implements CommandLineRunner {

    private static final String EXAMPLE_JSON_FILE = "s2ql_exemplar.json";

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private EmbeddingConfig embeddingConfig;

    private TypeReference<List<Exemplar>> valueTypeRef = new TypeReference<List<Exemplar>>() {
    };

    @Override
    public void run(String... args) {
        try {
            if (ComponentFactory.getLLMProxy() instanceof JavaLLMProxy) {
                loadDefaultExemplars();
            }
        } catch (Exception e) {
            log.error("Failed to init examples", e);
        }
    }

    public void addExemplars(List<Exemplar> exemplars, String collectionName) {
        List<TextSegment> queries = new ArrayList<>();
        for (int i = 0; i < exemplars.size(); i++) {
            Exemplar exemplar = exemplars.get(i);
            String question = exemplar.getQuestion();
            Map<String, Object> metaDataMap = JsonUtil.toMap(JsonUtil.toString(exemplar), String.class, Object.class);
            TextSegment embeddingQuery = TextSegment.from(question, new Metadata(metaDataMap));
            TextSegmentConvert.addQueryId(embeddingQuery, String.valueOf(i));
            queries.add(embeddingQuery);
        }
        embeddingService.addQuery(collectionName, queries);
    }

    public List<Map<String, String>> recallExemplars(String queryText, int maxResults) {
        String collectionName = embeddingConfig.getText2sqlCollectionName();
        RetrieveQuery retrieveQuery = RetrieveQuery.builder().queryTextsList(Collections.singletonList(queryText))
                .queryEmbeddings(null).build();

        List<RetrieveQueryResult> resultList = embeddingService.retrieveQuery(collectionName, retrieveQuery,
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

    private void loadDefaultExemplars() throws IOException {
        ClassPathResource resource = new ClassPathResource(EXAMPLE_JSON_FILE);
        InputStream inputStream = resource.getInputStream();
        List<Exemplar> examples = JsonUtil.INSTANCE.getObjectMapper().readValue(inputStream, valueTypeRef);
        String collectionName = embeddingConfig.getText2sqlCollectionName();
        addExemplars(examples, collectionName);
    }

}
