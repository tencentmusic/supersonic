package com.tencent.supersonic.headless.core.chat.mapper;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.embedding.Retrieval;
import com.tencent.supersonic.common.util.embedding.RetrieveQuery;
import com.tencent.supersonic.common.util.embedding.RetrieveQueryResult;
import com.tencent.supersonic.headless.core.chat.knowledge.EmbeddingResult;
import com.tencent.supersonic.headless.core.chat.knowledge.MetaEmbeddingService;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.supersonic.headless.core.config.MapperConfig.EMBEDDING_MAPPER_BATCH;
import static com.tencent.supersonic.headless.core.config.MapperConfig.EMBEDDING_MAPPER_MAX;
import static com.tencent.supersonic.headless.core.config.MapperConfig.EMBEDDING_MAPPER_MIN;
import static com.tencent.supersonic.headless.core.config.MapperConfig.EMBEDDING_MAPPER_NUMBER;
import static com.tencent.supersonic.headless.core.config.MapperConfig.EMBEDDING_MAPPER_ROUND_NUMBER;
import static com.tencent.supersonic.headless.core.config.MapperConfig.EMBEDDING_MAPPER_THRESHOLD;
import static com.tencent.supersonic.headless.core.config.MapperConfig.EMBEDDING_MAPPER_THRESHOLD_MIN;

/**
 * EmbeddingMatchStrategy uses vector database to perform
 * similarity search against the embeddings of schema elements.
 */
@Service
@Slf4j
public class EmbeddingMatchStrategy extends BaseMatchStrategy<EmbeddingResult> {

    @Autowired
    private MetaEmbeddingService metaEmbeddingService;

    @Override
    public boolean needDelete(EmbeddingResult oneRoundResult, EmbeddingResult existResult) {
        return getMapKey(oneRoundResult).equals(getMapKey(existResult))
                && existResult.getDistance() > oneRoundResult.getDistance();
    }

    @Override
    public String getMapKey(EmbeddingResult a) {
        return a.getName() + Constants.UNDERLINE + a.getId();
    }

    @Override
    public void detectByStep(QueryContext queryContext, Set<EmbeddingResult> existResults,
                             Set<Long> detectDataSetIds, String detectSegment, int offset) {

    }

    @Override
    protected void detectByBatch(QueryContext queryContext, Set<EmbeddingResult> results,
                                 Set<Long> detectDataSetIds, Set<String> detectSegments) {
        int embedddingMapperMin = Integer.valueOf(mapperConfig.getParameterValue(EMBEDDING_MAPPER_MIN));
        int embedddingMapperMax = Integer.valueOf(mapperConfig.getParameterValue(EMBEDDING_MAPPER_MAX));
        int embeddingMapperBatch = Integer.valueOf(mapperConfig.getParameterValue(EMBEDDING_MAPPER_BATCH));

        List<String> queryTextsList = detectSegments.stream()
                .map(detectSegment -> detectSegment.trim())
                .filter(detectSegment -> StringUtils.isNotBlank(detectSegment)
                        && detectSegment.length() >= embedddingMapperMin
                        && detectSegment.length() <= embedddingMapperMax)
                .collect(Collectors.toList());

        List<List<String>> queryTextsSubList = Lists.partition(queryTextsList,
                embeddingMapperBatch);

        for (List<String> queryTextsSub : queryTextsSubList) {
            detectByQueryTextsSub(results, detectDataSetIds, queryTextsSub, queryContext);
        }
    }

    private void detectByQueryTextsSub(Set<EmbeddingResult> results, Set<Long> detectDataSetIds,
                                       List<String> queryTextsSub, QueryContext queryContext) {
        Map<Long, List<Long>> modelIdToDataSetIds = queryContext.getModelIdToDataSetIds();
        double embeddingThreshold = Double.valueOf(mapperConfig.getParameterValue(EMBEDDING_MAPPER_THRESHOLD));
        double embeddingThresholdMin = Double.valueOf(mapperConfig.getParameterValue(EMBEDDING_MAPPER_THRESHOLD_MIN));
        double threshold = getThreshold(embeddingThreshold, embeddingThresholdMin, queryContext.getMapModeEnum());

        // step1. build query params
        RetrieveQuery retrieveQuery = RetrieveQuery.builder().queryTextsList(queryTextsSub).build();

        // step2. retrieveQuery by detectSegment
        int embeddingNumber = Integer.valueOf(mapperConfig.getParameterValue(EMBEDDING_MAPPER_NUMBER));
        List<RetrieveQueryResult> retrieveQueryResults = metaEmbeddingService.retrieveQuery(
                retrieveQuery, embeddingNumber, modelIdToDataSetIds, detectDataSetIds);

        if (CollectionUtils.isEmpty(retrieveQueryResults)) {
            return;
        }
        // step3. build EmbeddingResults
        List<EmbeddingResult> collect = retrieveQueryResults.stream()
                .map(retrieveQueryResult -> {
                    List<Retrieval> retrievals = retrieveQueryResult.getRetrieval();
                    if (CollectionUtils.isNotEmpty(retrievals)) {
                        retrievals.removeIf(retrieval -> {
                            if (!retrieveQueryResult.getQuery().contains(retrieval.getQuery())) {
                                return retrieval.getDistance() > 1 - threshold;
                            }
                            return false;
                        });
                    }
                    return retrieveQueryResult;
                })
                .filter(retrieveQueryResult -> CollectionUtils.isNotEmpty(retrieveQueryResult.getRetrieval()))
                .flatMap(retrieveQueryResult -> retrieveQueryResult.getRetrieval().stream()
                        .map(retrieval -> {
                            EmbeddingResult embeddingResult = new EmbeddingResult();
                            BeanUtils.copyProperties(retrieval, embeddingResult);
                            embeddingResult.setDetectWord(retrieveQueryResult.getQuery());
                            embeddingResult.setName(retrieval.getQuery());
                            Map<String, String> convertedMap = retrieval.getMetadata().entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));
                            embeddingResult.setMetadata(convertedMap);
                            return embeddingResult;
                        }))
                .collect(Collectors.toList());

        // step4. select mapResul in one round
        int embeddingRoundNumber = Integer.valueOf(mapperConfig.getParameterValue(EMBEDDING_MAPPER_ROUND_NUMBER));
        int roundNumber = embeddingRoundNumber * queryTextsSub.size();
        List<EmbeddingResult> oneRoundResults = collect.stream()
                .sorted(Comparator.comparingDouble(EmbeddingResult::getDistance))
                .limit(roundNumber)
                .collect(Collectors.toList());
        selectResultInOneRound(results, oneRoundResults);
    }

}
