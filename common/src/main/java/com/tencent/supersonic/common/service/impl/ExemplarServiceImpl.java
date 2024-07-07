package com.tencent.supersonic.common.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.service.EmbeddingService;
import com.tencent.supersonic.common.service.ExemplarService;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.pojo.SqlExemplar;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@Slf4j
@Order(0)
public class ExemplarServiceImpl implements ExemplarService, CommandLineRunner {

    private static final String SYS_EXEMPLAR_FILE = "s2-exemplar.json";

    private TypeReference<List<SqlExemplar>> valueTypeRef = new TypeReference<List<SqlExemplar>>() {
    };

    private final ObjectMapper objectMapper = JsonUtil.INSTANCE.getObjectMapper();

    @Autowired
    private EmbeddingConfig embeddingConfig;

    @Autowired
    private EmbeddingService embeddingService;

    public void storeExemplar(String collection, SqlExemplar exemplar) {
        Metadata metadata = Metadata.from(JsonUtil.toMap(JsonUtil.toString(exemplar),
                String.class, Object.class));
        TextSegment segment = TextSegment.from(exemplar.getQuestion(), metadata);

        embeddingService.addQuery(collection, Lists.newArrayList(segment));
    }

    public void removeExemplar(String collection, SqlExemplar exemplar) {
        Metadata metadata = Metadata.from(JsonUtil.toMap(JsonUtil.toString(exemplar),
                String.class, Object.class));
        TextSegment segment = TextSegment.from(exemplar.getQuestion(), metadata);

        embeddingService.deleteQuery(collection, Lists.newArrayList(segment));
    }

    public List<SqlExemplar> recallExemplars(String query, int num) {
        String collection = embeddingConfig.getText2sqlCollectionName();
        return recallExemplars(collection, query, num);
    }

    public List<SqlExemplar> recallExemplars(String collection, String query, int num) {
        List<SqlExemplar> exemplars = Lists.newArrayList();
        RetrieveQuery retrieveQuery = RetrieveQuery.builder()
                .queryTextsList(Lists.newArrayList(query))
                .build();
        List<RetrieveQueryResult> results = embeddingService.retrieveQuery(collection, retrieveQuery, num);
        results.stream().forEach(ret -> {
            ret.getRetrieval().stream().forEach(r -> {
                exemplars.add(JsonUtil.mapToObject(r.getMetadata(), SqlExemplar.class));
            });
        });

        return exemplars;
    }

    @Override
    public void run(String... args) {
        try {
            loadSysExemplars();
        } catch (IOException e) {
            log.error("Failed to load system exemplars", e);
        }
    }

    private void loadSysExemplars() throws IOException {
        ClassPathResource resource = new ClassPathResource(SYS_EXEMPLAR_FILE);
        InputStream inputStream = resource.getInputStream();
        List<SqlExemplar> exemplars = objectMapper.readValue(inputStream, valueTypeRef);
        String collection = embeddingConfig.getText2sqlCollectionName();
        exemplars.stream().forEach(e -> storeExemplar(collection, e));
    }

}
