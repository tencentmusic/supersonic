package com.tencent.supersonic.chat.server.processor.execute;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.chat.server.pojo.ChatExecuteContext;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.store.embedding.Retrieval;
import dev.langchain4j.store.embedding.RetrieveQuery;
import dev.langchain4j.store.embedding.RetrieveQueryResult;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.chat.knowledge.MetaEmbeddingService;
import java.util.Objects;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MetricRecommendProcessor fills recommended metrics based on embedding similarity.
 */
public class MetricRecommendProcessor implements ExecuteResultProcessor {

    private static final int METRIC_RECOMMEND_SIZE = 5;

    @Override
    public void process(ChatExecuteContext chatExecuteContext, QueryResult queryResult) {
        fillSimilarMetric(chatExecuteContext.getParseInfo());
    }

    private void fillSimilarMetric(SemanticParseInfo parseInfo) {
        if (!parseInfo.getQueryType().equals(QueryType.METRIC)
                || parseInfo.getMetrics().size() > METRIC_RECOMMEND_SIZE
                || CollectionUtils.isEmpty(parseInfo.getMetrics())) {
            return;
        }
        List<String> metricNames = Collections.singletonList(parseInfo.getMetrics().iterator().next().getName());
        Map<String, String> filterCondition = new HashMap<>();
        filterCondition.put("modelId", parseInfo.getMetrics().iterator().next().getDataSet().toString());
        filterCondition.put("type", SchemaElementType.METRIC.name());
        RetrieveQuery retrieveQuery = RetrieveQuery.builder().queryTextsList(metricNames)
                .filterCondition(filterCondition).queryEmbeddings(null).build();
        MetaEmbeddingService metaEmbeddingService = ContextUtils.getBean(MetaEmbeddingService.class);
        List<RetrieveQueryResult> retrieveQueryResults =
                metaEmbeddingService.retrieveQuery(retrieveQuery, METRIC_RECOMMEND_SIZE + 1, new HashMap<>(),
                        new HashSet<>());
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
                if (Objects.nonNull(retrieval.getMetadata().get("id"))) {
                    String idStr = retrieval.getMetadata().get("id").toString()
                            .replaceAll(DictWordType.NATURE_SPILT, "");
                    retrieval.getMetadata().put("id", idStr);
                }
                String metaStr = JSONObject.toJSONString(retrieval.getMetadata());
                SchemaElement schemaElement = JSONObject.parseObject(metaStr, SchemaElement.class);
                if (retrieval.getMetadata().containsKey("dataSetId")) {
                    String dataSetId = retrieval.getMetadata().get("dataSetId").toString()
                            .replace(Constants.UNDERLINE, "");
                    schemaElement.setDataSet(Long.parseLong(dataSetId));
                }
                schemaElement.setOrder(++metricOrder);
                parseInfo.getMetrics().add(schemaElement);
            }
        }
    }

}
