package com.tencent.supersonic.common.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.Text2SQLExemplar;
import com.tencent.supersonic.common.service.EmbeddingService;
import com.tencent.supersonic.common.service.ExemplarService;
import com.tencent.supersonic.common.util.JsonUtil;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import dev.langchain4j.store.embedding.TextSegmentConvert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
@Slf4j
@Order(0)
public class ExemplarServiceImpl implements ExemplarService, CommandLineRunner {

    private static final String SYS_EXEMPLAR_FILE = "s2-exemplar.json";

    private TypeReference<List<Text2SQLExemplar>> valueTypeRef =
            new TypeReference<List<Text2SQLExemplar>>() {};

    private final ObjectMapper objectMapper = JsonUtil.INSTANCE.getObjectMapper();

    @Autowired
    private EmbeddingConfig embeddingConfig;

    @Autowired
    private EmbeddingService embeddingService;

    public void storeExemplar(String collection, Text2SQLExemplar exemplar) {
        Metadata metadata = Metadata
                .from(JsonUtil.toMap(JsonUtil.toString(exemplar), String.class, Object.class));
        TextSegment segment = TextSegment.from(exemplar.getQuestion(), metadata);
        TextSegmentConvert.addQueryId(segment, exemplar.getQuestion());

        embeddingService.addQuery(collection, Lists.newArrayList(segment));
    }

    public void removeExemplar(String collection, Text2SQLExemplar exemplar) {
        Metadata metadata = Metadata
                .from(JsonUtil.toMap(JsonUtil.toString(exemplar), String.class, Object.class));
        TextSegment segment = TextSegment.from(exemplar.getQuestion(), metadata);
        TextSegmentConvert.addQueryId(segment, exemplar.getQuestion());

        embeddingService.deleteQuery(collection, Lists.newArrayList(segment));
    }

    public List<Text2SQLExemplar> recallExemplars(String query, int num) {
        String collection = embeddingConfig.getText2sqlCollectionName();
        return recallExemplars(collection, query, num);
    }

    public List<Text2SQLExemplar> recallExemplars(String collection, String query, int num) {
        List<Text2SQLExemplar> exemplars = Lists.newArrayList();
        RetrieveQuery retrieveQuery =
                RetrieveQuery.builder().queryTextsList(Lists.newArrayList(query)).build();
        List<RetrieveQueryResult> results =
                embeddingService.retrieveQuery(collection, retrieveQuery, num);
        results.forEach(ret -> {
            ret.getRetrieval().forEach(r -> {
                exemplars.add(JsonUtil.mapToObject(r.getMetadata(), Text2SQLExemplar.class));
            });
        });

        return exemplars;
    }

    @Override
    public void run(String... args) {
        loadSysExemplars();
    }

    public void loadSysExemplars() {
        try {
            ClassPathResource resource = new ClassPathResource(SYS_EXEMPLAR_FILE);
            InputStream inputStream = resource.getInputStream();
            List<Text2SQLExemplar> exemplars = objectMapper.readValue(inputStream, valueTypeRef);
            String collection = embeddingConfig.getText2sqlCollectionName();
            exemplars.stream().forEach(e -> storeExemplar(collection, e));
        } catch (Exception e) {
            log.error("Failed to load system exemplars", e);
        }
    }
}
