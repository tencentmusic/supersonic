package com.tencent.supersonic.headless.chat.mapper;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.EmbeddingResult;
import com.tencent.supersonic.headless.chat.knowledge.MetaEmbeddingService;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.tencent.supersonic.headless.chat.mapper.MapperConfig.EMBEDDING_MAPPER_NUMBER;
import static com.tencent.supersonic.headless.chat.mapper.MapperConfig.EMBEDDING_MAPPER_ROUND_NUMBER;
import static com.tencent.supersonic.headless.chat.mapper.MapperConfig.EMBEDDING_MAPPER_THRESHOLD;

/**
 * EmbeddingMatchStrategy uses vector database to perform similarity search against the embeddings
 * of schema elements.
 */
@Service
@Slf4j
public class EmbeddingMatchStrategy extends BatchMatchStrategy<EmbeddingResult> {

    @Autowired
    private MetaEmbeddingService metaEmbeddingService;

    @Override
    public List<EmbeddingResult> detectByBatch(ChatQueryContext chatQueryContext,
            Set<Long> detectDataSetIds, Set<String> detectSegments) {
        Set<EmbeddingResult> results = ConcurrentHashMap.newKeySet();
        int embeddingMapperBatch = Integer
                .valueOf(mapperConfig.getParameterValue(MapperConfig.EMBEDDING_MAPPER_BATCH));

        List<String> queryTextsList =
                detectSegments.stream().map(detectSegment -> detectSegment.trim())
                        .filter(detectSegment -> StringUtils.isNotBlank(detectSegment))
                        .collect(Collectors.toList());

        List<List<String>> queryTextsSubList =
                Lists.partition(queryTextsList, embeddingMapperBatch);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (List<String> queryTextsSub : queryTextsSubList) {
            tasks.add(createTask(chatQueryContext, detectDataSetIds, queryTextsSub, results));
        }
        executeTasks(tasks);
        return new ArrayList<>(results);
    }

    private Callable<Void> createTask(ChatQueryContext chatQueryContext, Set<Long> detectDataSetIds,
            List<String> queryTextsSub, Set<EmbeddingResult> results) {
        return () -> {
            List<EmbeddingResult> oneRoundResults =
                    detectByQueryTextsSub(detectDataSetIds, queryTextsSub, chatQueryContext);
            synchronized (results) {
                selectResultInOneRound(results, oneRoundResults);
            }
            return null;
        };
    }

    private List<EmbeddingResult> detectByQueryTextsSub(Set<Long> detectDataSetIds,
            List<String> queryTextsSub, ChatQueryContext chatQueryContext) {
        Map<Long, List<Long>> modelIdToDataSetIds = chatQueryContext.getModelIdToDataSetIds();
        double threshold =
                Double.valueOf(mapperConfig.getParameterValue(EMBEDDING_MAPPER_THRESHOLD));

        // step1. build query params
        RetrieveQuery retrieveQuery = RetrieveQuery.builder().queryTextsList(queryTextsSub).build();

        // step2. retrieveQuery by detectSegment
        int embeddingNumber =
                Integer.valueOf(mapperConfig.getParameterValue(EMBEDDING_MAPPER_NUMBER));
        List<RetrieveQueryResult> retrieveQueryResults = metaEmbeddingService.retrieveQuery(
                retrieveQuery, embeddingNumber, modelIdToDataSetIds, detectDataSetIds);

        if (CollectionUtils.isEmpty(retrieveQueryResults)) {
            return new ArrayList<>();
        }
        // step3. build EmbeddingResults
        List<EmbeddingResult> collect = retrieveQueryResults.stream().map(retrieveQueryResult -> {
            List<Retrieval> retrievals = retrieveQueryResult.getRetrieval();
            if (CollectionUtils.isNotEmpty(retrievals)) {
                retrievals.removeIf(retrieval -> {
                    if (!retrieveQueryResult.getQuery().contains(retrieval.getQuery())) {
                        return retrieval.getSimilarity() < threshold;
                    }
                    return false;
                });
            }
            return retrieveQueryResult;
        }).filter(retrieveQueryResult -> CollectionUtils
                .isNotEmpty(retrieveQueryResult.getRetrieval()))
                .flatMap(retrieveQueryResult -> retrieveQueryResult.getRetrieval().stream()
                        .map(retrieval -> {
                            EmbeddingResult embeddingResult = new EmbeddingResult();
                            BeanUtils.copyProperties(retrieval, embeddingResult);
                            embeddingResult.setDetectWord(retrieveQueryResult.getQuery());
                            embeddingResult.setName(retrieval.getQuery());
                            Map<String, String> convertedMap = retrieval.getMetadata().entrySet()
                                    .stream().collect(Collectors.toMap(Map.Entry::getKey,
                                            entry -> entry.getValue().toString()));
                            embeddingResult.setMetadata(convertedMap);
                            return embeddingResult;
                        }))
                .collect(Collectors.toList());

        // step4. select mapResul in one round
        int embeddingRoundNumber =
                Integer.valueOf(mapperConfig.getParameterValue(EMBEDDING_MAPPER_ROUND_NUMBER));
        int roundNumber = embeddingRoundNumber * queryTextsSub.size();
        return collect.stream().sorted(Comparator.comparingDouble(EmbeddingResult::getSimilarity))
                .limit(roundNumber).collect(Collectors.toList());
    }
}
