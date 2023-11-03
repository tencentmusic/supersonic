package com.tencent.supersonic.chat.responder.execute;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.embedding.EmbeddingUtils;
import com.tencent.supersonic.common.util.embedding.Retrieval;
import com.tencent.supersonic.common.util.embedding.RetrieveQuery;
import com.tencent.supersonic.common.util.embedding.RetrieveQueryResult;
import com.tencent.supersonic.semantic.model.domain.listener.MetaEmbeddingListener;
import org.springframework.util.CollectionUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SimilarMetricExecuteResponder implements ExecuteResponder {


    @Override
    public void fillResponse(QueryResult queryResult, SemanticParseInfo semanticParseInfo, ExecuteQueryReq queryReq) {
        fillSimilarMetric(queryResult.getChatContext());
    }

    private void fillSimilarMetric(SemanticParseInfo parseInfo) {
        if (!QueryManager.isMetricQuery(parseInfo.getQueryMode())
                || CollectionUtils.isEmpty(parseInfo.getMetrics())) {
            return;
        }
        List<String> metricNames = Collections.singletonList(parseInfo.getMetrics().iterator().next().getName());
        Map<String, String> filterCondition = new HashMap<>();
        filterCondition.put("modelId", parseInfo.getModelId().toString());
        filterCondition.put("type", SchemaElementType.METRIC.name());
        RetrieveQuery retrieveQuery = RetrieveQuery.builder().queryTextsList(metricNames)
                .filterCondition(filterCondition).queryEmbeddings(null).build();
        EmbeddingUtils embeddingUtils = ContextUtils.getBean(EmbeddingUtils.class);
        List<RetrieveQueryResult> retrieveQueryResults = embeddingUtils.retrieveQuery(
                MetaEmbeddingListener.COLLECTION_NAME, retrieveQuery, 5);
        if (CollectionUtils.isEmpty(retrieveQueryResults)) {
            return;
        }
        List<Retrieval> retrievals = retrieveQueryResults.stream()
                .flatMap(retrieveQueryResult -> retrieveQueryResult.getRetrieval().stream())
                .sorted(Comparator.comparingDouble(Retrieval::getDistance).reversed())
                .distinct().collect(Collectors.toList());
        Set<Long> metricIds = parseInfo.getMetrics().stream().map(SchemaElement::getId).collect(Collectors.toSet());
        int metricOrder = 0;
        for (SchemaElement metric : parseInfo.getMetrics()) {
            metric.setOrder(metricOrder++);
        }
        for (Retrieval retrieval : retrievals) {
            if (!metricIds.contains(retrieval.getId())) {
                SchemaElement schemaElement = JSONObject.parseObject(JSONObject.toJSONString(retrieval.getMetadata()),
                        SchemaElement.class);
                if (retrieval.getMetadata().containsKey("modelId")) {
                    schemaElement.setModel(Long.parseLong(retrieval.getMetadata().get("modelId")));
                }
                schemaElement.setOrder(metricOrder++);
                parseInfo.getMetrics().add(schemaElement);
            }
        }
    }

}
