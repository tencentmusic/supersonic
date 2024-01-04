package com.tencent.supersonic.chat.server.processor.execute;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.ExecuteQueryReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.ComponentFactory;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.embedding.Retrieval;
import com.tencent.supersonic.common.util.embedding.RetrieveQuery;
import com.tencent.supersonic.common.util.embedding.RetrieveQueryResult;
import com.tencent.supersonic.common.util.embedding.S2EmbeddingStore;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;

/**
 * MetricRecommendProcessor fills recommended metrics based on embedding similarity.
 */
public class MetricRecommendProcessor implements ExecuteResultProcessor {

    private static final int METRIC_RECOMMEND_SIZE = 5;

    private S2EmbeddingStore s2EmbeddingStore = ComponentFactory.getS2EmbeddingStore();

    @Override
    public void process(QueryResult queryResult, SemanticParseInfo semanticParseInfo, ExecuteQueryReq queryReq) {
        fillSimilarMetric(queryResult.getChatContext());
    }

    private void fillSimilarMetric(SemanticParseInfo parseInfo) {
        if (!parseInfo.getQueryType().equals(QueryType.METRIC)
                || parseInfo.getMetrics().size() > METRIC_RECOMMEND_SIZE
                || CollectionUtils.isEmpty(parseInfo.getMetrics())) {
            return;
        }
        List<String> metricNames = Collections.singletonList(parseInfo.getMetrics().iterator().next().getName());
        Map<String, String> filterCondition = new HashMap<>();
        filterCondition.put("modelId", parseInfo.getMetrics().iterator().next().getModel().toString());
        filterCondition.put("type", SchemaElementType.METRIC.name());
        RetrieveQuery retrieveQuery = RetrieveQuery.builder().queryTextsList(metricNames)
                .filterCondition(filterCondition).queryEmbeddings(null).build();

        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);

        List<RetrieveQueryResult> retrieveQueryResults = s2EmbeddingStore.retrieveQuery(
                embeddingConfig.getMetaCollectionName(), retrieveQuery, METRIC_RECOMMEND_SIZE + 1);
        if (CollectionUtils.isEmpty(retrieveQueryResults)) {
            return;
        }
        List<Retrieval> retrievals = retrieveQueryResults.stream()
                .flatMap(retrieveQueryResult -> retrieveQueryResult.getRetrieval().stream())
                .sorted(Comparator.comparingDouble(Retrieval::getDistance))
                .distinct().collect(Collectors.toList());
        Set<Long> metricIds = parseInfo.getMetrics().stream().map(SchemaElement::getId).collect(Collectors.toSet());
        int metricOrder = 0;
        for (SchemaElement metric : parseInfo.getMetrics()) {
            metric.setOrder(metricOrder++);
        }
        for (Retrieval retrieval : retrievals) {
            if (!metricIds.contains(Retrieval.getLongId(retrieval.getId()))) {
                SchemaElement schemaElement = JSONObject.parseObject(JSONObject.toJSONString(retrieval.getMetadata()),
                        SchemaElement.class);
                if (retrieval.getMetadata().containsKey("modelId")) {
                    String modelId = retrieval.getMetadata().get("modelId").toString();
                    schemaElement.setModel(Long.parseLong(modelId));
                }
                schemaElement.setOrder(++metricOrder);
                parseInfo.getMetrics().add(schemaElement);
            }
        }
    }

}
