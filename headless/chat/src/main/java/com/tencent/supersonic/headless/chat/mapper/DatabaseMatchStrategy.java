package com.tencent.supersonic.headless.chat.mapper;


import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.QueryContext;
import com.tencent.supersonic.headless.chat.knowledge.DatabaseMapResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DatabaseMatchStrategy uses SQL LIKE operator to match schema elements.
 * It currently supports fuzzy matching against names and aliases.
 */
@Service
@Slf4j
public class DatabaseMatchStrategy extends BaseMatchStrategy<DatabaseMapResult> {

    private List<SchemaElement> allElements;

    @Override
    public Map<MatchText, List<DatabaseMapResult>> match(QueryContext queryContext, List<S2Term> terms,
                                                         Set<Long> detectDataSetIds) {
        this.allElements = getSchemaElements(queryContext);
        return super.match(queryContext, terms, detectDataSetIds);
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

    public void detectByStep(QueryContext queryContext, Set<DatabaseMapResult> existResults, Set<Long> detectDataSetIds,
                             String detectSegment, int offset) {
        if (StringUtils.isBlank(detectSegment)) {
            return;
        }

        Double metricDimensionThresholdConfig = getThreshold(queryContext);
        Map<String, Set<SchemaElement>> nameToItems = getNameToItems(allElements);

        for (Entry<String, Set<SchemaElement>> entry : nameToItems.entrySet()) {
            String name = entry.getKey();
            if (!name.contains(detectSegment)
                    || mapperHelper.getSimilarity(detectSegment, name) < metricDimensionThresholdConfig) {
                continue;
            }
            Set<SchemaElement> schemaElements = entry.getValue();
            if (!CollectionUtils.isEmpty(detectDataSetIds)) {
                schemaElements = schemaElements.stream()
                        .filter(schemaElement -> detectDataSetIds.contains(schemaElement.getDataSet()))
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
        Double threshold = Double.valueOf(mapperConfig.getParameterValue(MapperConfig.MAPPER_NAME_THRESHOLD));
        Double minThreshold = Double.valueOf(mapperConfig.getParameterValue(MapperConfig.MAPPER_NAME_THRESHOLD_MIN));

        Map<Long, List<SchemaElementMatch>> modelElementMatches = queryContext.getMapInfo().getDataSetElementMatches();

        boolean existElement = modelElementMatches.entrySet().stream().anyMatch(entry -> entry.getValue().size() >= 1);

        if (!existElement) {
            threshold = threshold / 2;
            log.debug("ModelElementMatches:{},not exist Element threshold reduce by half:{}",
                    modelElementMatches, threshold);
        }
        return getThreshold(threshold, minThreshold, queryContext.getMapModeEnum());
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
