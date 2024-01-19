package com.tencent.supersonic.chat.core.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.core.config.OptimizationConfig;
import com.tencent.supersonic.chat.core.knowledge.DatabaseMapResult;
import com.tencent.supersonic.common.pojo.Constants;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * DatabaseMatchStrategy uses SQL LIKE operator to match schema elements.
 * It currently supports fuzzy matching against names and aliases.
 */
@Service
@Slf4j
public class DatabaseMatchStrategy extends BaseMatchStrategy<DatabaseMapResult> {

    @Autowired
    private OptimizationConfig optimizationConfig;
    @Autowired
    private MapperHelper mapperHelper;
    private List<SchemaElement> allElements;

    @Override
    public Map<MatchText, List<DatabaseMapResult>> match(QueryContext queryContext, List<Term> terms,
            Set<Long> detectModelIds) {
        this.allElements = getSchemaElements(queryContext);
        return super.match(queryContext, terms, detectModelIds);
    }

    @Override
    public boolean needDelete(DatabaseMapResult oneRoundResult, DatabaseMapResult existResult) {
        return getMapKey(oneRoundResult).equals(getMapKey(existResult))
                && existResult.getDetectWord().length() < oneRoundResult.getDetectWord().length();
    }

    @Override
    public String getMapKey(DatabaseMapResult a) {
        return a.getName() + Constants.UNDERLINE + a.getSchemaElement().getId()
                + Constants.UNDERLINE + a.getSchemaElement().getName();
    }

    public void detectByStep(QueryContext queryContext, Set<DatabaseMapResult> existResults, Set<Long> detectModelIds,
            Integer startIndex, Integer index, int offset) {
        String detectSegment = queryContext.getQueryText().substring(startIndex, index);
        if (StringUtils.isBlank(detectSegment)) {
            return;
        }
        Set<Long> modelIds = mapperHelper.getModelIds(queryContext.getModelId(), queryContext.getAgent());

        Double metricDimensionThresholdConfig = getThreshold(queryContext);

        Map<String, Set<SchemaElement>> nameToItems = getNameToItems(allElements);

        for (Entry<String, Set<SchemaElement>> entry : nameToItems.entrySet()) {
            String name = entry.getKey();
            if (!name.contains(detectSegment)
                    || mapperHelper.getSimilarity(detectSegment, name) < metricDimensionThresholdConfig) {
                continue;
            }
            Set<SchemaElement> schemaElements = entry.getValue();
            if (!CollectionUtils.isEmpty(modelIds)) {
                schemaElements = schemaElements.stream()
                        .filter(schemaElement -> modelIds.contains(schemaElement.getModel()))
                        .collect(Collectors.toSet());
            }
            for (SchemaElement schemaElement : schemaElements) {
                DatabaseMapResult databaseMapResult = new DatabaseMapResult();
                databaseMapResult.setDetectWord(detectSegment);
                databaseMapResult.setName(schemaElement.getName());
                databaseMapResult.setSchemaElement(schemaElement);
                existResults.add(databaseMapResult);
            }
        }
    }

    private List<SchemaElement> getSchemaElements(QueryContext queryContext) {
        List<SchemaElement> allElements = new ArrayList<>();
        allElements.addAll(queryContext.getSemanticSchema().getDimensions());
        allElements.addAll(queryContext.getSemanticSchema().getMetrics());
        return allElements;
    }

    private Double getThreshold(QueryContext queryContext) {
        Double metricDimensionThresholdConfig = optimizationConfig.getMetricDimensionThresholdConfig();
        Double metricDimensionMinThresholdConfig = optimizationConfig.getMetricDimensionMinThresholdConfig();

        Map<Long, List<SchemaElementMatch>> modelElementMatches = queryContext.getMapInfo().getModelElementMatches();

        boolean existElement = modelElementMatches.entrySet().stream().anyMatch(entry -> entry.getValue().size() >= 1);

        if (!existElement) {
            double halfThreshold = metricDimensionThresholdConfig / 2;

            metricDimensionThresholdConfig = halfThreshold >= metricDimensionMinThresholdConfig ? halfThreshold
                    : metricDimensionMinThresholdConfig;
            log.info("ModelElementMatches:{} , not exist Element metricDimensionThresholdConfig reduce by half:{}",
                    modelElementMatches, metricDimensionThresholdConfig);
        }
        return metricDimensionThresholdConfig;
    }

    private Map<String, Set<SchemaElement>> getNameToItems(List<SchemaElement> models) {
        return models.stream().collect(
                Collectors.toMap(SchemaElement::getName, a -> {
                    Set<SchemaElement> result = new HashSet<>();
                    result.add(a);
                    return result;
                }, (k1, k2) -> {
                    k1.addAll(k2);
                    return k1;
                }));
    }
}
