package com.tencent.supersonic.chat.server.processor.execute;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.server.pojo.ChatExecuteContext;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.RelatedSchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.server.service.SemanticLayerService;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DimensionRecommendProcessor recommend some dimensions
 * related to metrics based on configuration
 */
public class DimensionRecommendProcessor implements ExecuteResultProcessor {

    private static final int recommend_dimension_size = 5;

    @Override
    public void process(ChatExecuteContext chatExecuteContext, QueryResult queryResult) {
        SemanticParseInfo semanticParseInfo = chatExecuteContext.getParseInfo();
        if (!QueryType.METRIC.equals(semanticParseInfo.getQueryType())
                || CollectionUtils.isEmpty(semanticParseInfo.getMetrics())) {
            return;
        }
        SchemaElement element = semanticParseInfo.getMetrics().iterator().next();
        List<SchemaElement> dimensionRecommended = getDimensions(element.getId(), element.getDataSet());
        queryResult.setRecommendedDimensions(dimensionRecommended);
    }

    private List<SchemaElement> getDimensions(Long metricId, Long dataSetId) {
        SemanticLayerService semanticService = ContextUtils.getBean(SemanticLayerService.class);
        DataSetSchema dataSetSchema = semanticService.getDataSetSchema(dataSetId);
        List<Long> drillDownDimensions = Lists.newArrayList();
        Set<SchemaElement> metricElements = dataSetSchema.getMetrics();
        if (!CollectionUtils.isEmpty(metricElements)) {
            Optional<SchemaElement> metric = metricElements.stream().filter(schemaElement ->
                            metricId.equals(schemaElement.getId())
                                    && !CollectionUtils.isEmpty(schemaElement.getRelatedSchemaElements()))
                    .findFirst();
            if (metric.isPresent()) {
                drillDownDimensions = metric.get().getRelatedSchemaElements().stream()
                        .map(RelatedSchemaElement::getDimensionId).collect(Collectors.toList());
            }
        }
        final List<Long> drillDownDimensionsFinal = drillDownDimensions;
        return dataSetSchema.getDimensions().stream()
                .filter(dim -> filterDimension(drillDownDimensionsFinal, dim))
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .limit(recommend_dimension_size)
                .collect(Collectors.toList());
    }

    private boolean filterDimension(List<Long> drillDownDimensions, SchemaElement dimension) {
        if (Objects.isNull(dimension)) {
            return false;
        }
        if (!CollectionUtils.isEmpty(drillDownDimensions)) {
            return drillDownDimensions.contains(dimension.getId());
        }
        return Objects.nonNull(dimension.getUseCnt());
    }

}
