package com.tencent.supersonic.chat.mapper;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.config.OptimizationConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.embedding.EmbeddingUtils;
import com.tencent.supersonic.common.util.embedding.Retrieval;
import com.tencent.supersonic.common.util.embedding.RetrieveQuery;
import com.tencent.supersonic.common.util.embedding.RetrieveQueryResult;
import com.tencent.supersonic.knowledge.dictionary.builder.BaseWordBuilder;
import com.tencent.supersonic.semantic.model.domain.listener.MetaEmbeddingListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/***
 * A mapper that is capable of semantic understanding of text.
 */
@Slf4j
public class EmbeddingMapper extends BaseMapper {

    @Override
    public void work(QueryContext queryContext) {
        //1. query from embedding by queryText
        String queryText = queryContext.getRequest().getQueryText();

        EmbeddingUtils embeddingUtils = ContextUtils.getBean(EmbeddingUtils.class);
        OptimizationConfig optimizationConfig = ContextUtils.getBean(OptimizationConfig.class);

        int embeddingNumber = optimizationConfig.getEmbeddingMapperNumber();
        Double distance = optimizationConfig.getEmbeddingMapperDistanceThreshold();

        RetrieveQuery retrieveQuery = RetrieveQuery.builder()
                .queryTextsList(Collections.singletonList(queryText))
                .filterCondition(new HashMap<>())
                .queryEmbeddings(null)
                .build();

        List<RetrieveQueryResult> retrieveQueryResults = embeddingUtils.retrieveQuery(
                MetaEmbeddingListener.COLLECTION_NAME, retrieveQuery, embeddingNumber);

        if (CollectionUtils.isEmpty(retrieveQueryResults)) {
            return;
        }
        List<RetrieveQueryResult> collect = retrieveQueryResults.stream()
                .map(retrieveQueryResult -> {
                    List<Retrieval> retrievals = retrieveQueryResult.getRetrieval();
                    if (CollectionUtils.isNotEmpty(retrievals)) {
                        retrievals.removeIf(retrieval -> retrieval.getDistance() < distance);
                    }
                    return retrieveQueryResult;
                })
                .filter(retrieveQueryResult -> CollectionUtils.isNotEmpty(retrieveQueryResult.getRetrieval()))
                .collect(Collectors.toList());

        //2. build SchemaElementMatch by info
        for (RetrieveQueryResult retrieveQueryResult : collect) {
            List<Retrieval> retrievals = retrieveQueryResult.getRetrieval();
            for (Retrieval retrieval : retrievals) {
                Long elementId = retrieval.getId();

                SchemaElement schemaElement = JSONObject.parseObject(JSONObject.toJSONString(retrieval.getMetadata()),
                        SchemaElement.class);

                String modelIdStr = retrieval.getMetadata().get("modelId");
                if (StringUtils.isBlank(modelIdStr)) {
                    continue;
                }
                long modelId = Long.parseLong(modelIdStr);

                schemaElement = getSchemaElement(modelId, schemaElement.getType(), elementId);

                SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                        .element(schemaElement)
                        .frequency(BaseWordBuilder.DEFAULT_FREQUENCY)
                        .word(schemaElement.getName())
                        .similarity(retrieval.getDistance())
                        .detectWord(schemaElement.getName())
                        .build();
                //3. add to mapInfo
                addToSchemaMap(queryContext.getMapInfo(), modelId, schemaElementMatch);
            }
        }

    }
}
