package com.tencent.supersonic.chat.server.processor.execute;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.RelatedSchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DimensionRecommendProcessor recommend some dimensions related to metrics based on configuration
 */
public class DimensionRecommendProcessor implements ExecuteResultProcessor {

    private static final int recommend_dimension_size = 5;

    @Override
    public boolean accept(ExecuteContext executeContext) {
        SemanticParseInfo semanticParseInfo = executeContext.getParseInfo();
        return QueryType.AGGREGATE.equals(semanticParseInfo.getQueryType())
                && !CollectionUtils.isEmpty(semanticParseInfo.getMetrics());
    }

    @Override
    public void process(ExecuteContext executeContext) {
        QueryResult queryResult = executeContext.getResponse();
        SemanticParseInfo semanticParseInfo = executeContext.getParseInfo();
        Long dataSetId = semanticParseInfo.getDataSetId();
        Optional<SchemaElement> firstMetric = semanticParseInfo.getMetrics().stream().findFirst();
        List<SchemaElement> dimensionRecommended =
                getDimensions(firstMetric.get().getId(), dataSetId);
        queryResult.setRecommendedDimensions(dimensionRecommended);
    }

    private List<SchemaElement> getDimensions(Long metricId, Long dataSetId) {
        SemanticLayerService semanticService = ContextUtils.getBean(SemanticLayerService.class);
        DataSetSchema dataSetSchema = semanticService.getDataSetSchema(dataSetId);
        if (dataSetSchema == null) {
            return Lists.newArrayList();
        }
        SchemaElement metric = dataSetSchema.getElement(SchemaElementType.METRIC, metricId);
        if (!CollectionUtils.isEmpty(metric.getRelatedSchemaElements())) {
            List<Long> drillDownDimensions = metric.getRelatedSchemaElements().stream()
                    .map(RelatedSchemaElement::getDimensionId).collect(Collectors.toList());
            return dataSetSchema.getDimensions().stream()
                    .filter(dim -> filterDimension(drillDownDimensions, dim))
                    .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                    .limit(recommend_dimension_size).collect(Collectors.toList());
        }
        return Lists.newArrayList();
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
