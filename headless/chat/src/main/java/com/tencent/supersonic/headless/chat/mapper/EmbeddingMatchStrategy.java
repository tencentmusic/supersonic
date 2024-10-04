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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.supersonic.headless.chat.mapper.MapperConfig.EMBEDDING_MAPPER_NUMBER;
import static com.tencent.supersonic.headless.chat.mapper.MapperConfig.EMBEDDING_MAPPER_ROUND_NUMBER;
import static com.tencent.supersonic.headless.chat.mapper.MapperConfig.EMBEDDING_MAPPER_THRESHOLD;
import static com.tencent.supersonic.headless.chat.mapper.MapperConfig.EMBEDDING_MAPPER_THRESHOLD_MIN;

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
        Set<EmbeddingResult> results = new HashSet<>();
        int embeddingMapperBatch = Integer
                .valueOf(mapperConfig.getParameterValue(MapperConfig.EMBEDDING_MAPPER_BATCH));

        List<String> queryTextsList =
                detectSegments.stream().map(detectSegment -> detectSegment.trim())
                        .filter(detectSegment -> StringUtils.isNotBlank(detectSegment))
                        .collect(Collectors.toList());

        List<List<String>> queryTextsSubList =
                Lists.partition(queryTextsList, embeddingMapperBatch);

        for (List<String> queryTextsSub : queryTextsSubList) {
            List<EmbeddingResult> oneRoundResults =
                    detectByQueryTextsSub(detectDataSetIds, queryTextsSub, chatQueryContext);
            selectResultInOneRound(results, oneRoundResults);
        }
        return new ArrayList<>(results);
    }

    private List<EmbeddingResult> detectByQueryTextsSub(Set<Long> detectDataSetIds,
            List<String> queryTextsSub, ChatQueryContext chatQueryContext) {
        Map<Long, List<Long>> modelIdToDataSetIds = chatQueryContext.getModelIdToDataSetIds();
        double embeddingThreshold =
                Double.valueOf(mapperConfig.getParameterValue(EMBEDDING_MAPPER_THRESHOLD));
        double embeddingThresholdMin =
                Double.valueOf(mapperConfig.getParameterValue(EMBEDDING_MAPPER_THRESHOLD_MIN));
        double threshold = getThreshold(embeddingThreshold, embeddingThresholdMin,
                chatQueryContext.getMapModeEnum());

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
