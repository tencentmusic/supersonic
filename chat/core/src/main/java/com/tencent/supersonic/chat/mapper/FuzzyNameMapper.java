package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.knowledge.utils.HanlpHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class FuzzyNameMapper implements SchemaMapper {

    @Override
    public void map(QueryContext queryContext) {

        log.debug("before db mapper,mapInfo:{}", queryContext.getMapInfo());

        List<Term> terms = HanlpHelper.getTerms(queryContext.getRequest().getQueryText());

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();

        detectAndAddToSchema(queryContext, terms, semanticSchema.getDimensions(), SchemaElementType.DIMENSION);

        detectAndAddToSchema(queryContext, terms, semanticSchema.getMetrics(), SchemaElementType.METRIC);

        log.debug("after db mapper,mapInfo:{}", queryContext.getMapInfo());
    }

    private void detectAndAddToSchema(QueryContext queryContext, List<Term> terms, List<SchemaElement> domains,
                                      SchemaElementType schemaElementType) {
        try {

            Map<String, Set<SchemaElement>> domainResultSet = getResultSet(queryContext, terms, domains);

            addToSchemaMapInfo(domainResultSet, queryContext.getMapInfo(), schemaElementType);

        } catch (Exception e) {
            log.error("detectAndAddToSchema error", e);
        }
    }

    private Map<String, Set<SchemaElement>> getResultSet(QueryContext queryContext, List<Term> terms,
                                                         List<SchemaElement> domains) {

        String queryText = queryContext.getRequest().getQueryText();

        MapperHelper mapperHelper = ContextUtils.getBean(MapperHelper.class);

        Double metricDimensionThresholdConfig = getThreshold(queryContext, mapperHelper);

        Map<String, Set<SchemaElement>> nameToItems = getNameToItems(domains);

        Map<Integer, Integer> regOffsetToLength = terms.stream().sorted(Comparator.comparing(Term::length))
                .collect(Collectors.toMap(Term::getOffset, term -> term.word.length(), (value1, value2) -> value2));

        Map<String, Set<SchemaElement>> domainResultSet = new HashMap<>();
        for (Integer startIndex = 0; startIndex <= queryText.length() - 1; ) {
            for (Integer endIndex = startIndex; endIndex <= queryText.length(); ) {
                endIndex = mapperHelper.getStepIndex(regOffsetToLength, endIndex);
                if (endIndex > queryText.length()) {
                    continue;
                }
                String detectSegment = queryText.substring(startIndex, endIndex);

                for (Entry<String, Set<SchemaElement>> entry : nameToItems.entrySet()) {
                    String name = entry.getKey();
                    Set<SchemaElement> schemaElements = entry.getValue();
                    if (!name.contains(detectSegment)
                            || mapperHelper.getSimilarity(detectSegment, name) < metricDimensionThresholdConfig) {
                        continue;
                    }
                    Set<SchemaElement> preSchemaElements = domainResultSet.putIfAbsent(detectSegment,
                            schemaElements);
                    if (Objects.nonNull(preSchemaElements)) {
                        preSchemaElements.addAll(schemaElements);
                    }
                }
            }
            startIndex = mapperHelper.getStepIndex(regOffsetToLength, startIndex);
        }
        return domainResultSet;
    }

    private Double getThreshold(QueryContext queryContext, MapperHelper mapperHelper) {

        Double metricDimensionThresholdConfig = mapperHelper.getMetricDimensionThresholdConfig();
        Double metricDimensionMinThresholdConfig = mapperHelper.getMetricDimensionMinThresholdConfig();

        Map<Long, List<SchemaElementMatch>> domainElementMatches = queryContext.getMapInfo()
                .getDomainElementMatches();
        boolean existElement = domainElementMatches.entrySet().stream()
                .anyMatch(entry -> entry.getValue().size() >= 1);

        if (!existElement) {
            double halfThreshold = metricDimensionThresholdConfig / 2;

            metricDimensionThresholdConfig = halfThreshold >= metricDimensionMinThresholdConfig ? halfThreshold
                    : metricDimensionMinThresholdConfig;
            log.info("domainElementMatches:{} , not exist Element metricDimensionThresholdConfig reduce by half:{}",
                    domainElementMatches, metricDimensionThresholdConfig);
        }
        return metricDimensionThresholdConfig;
    }

    private Map<String, Set<SchemaElement>> getNameToItems(List<SchemaElement> domains) {
        return domains.stream().collect(
                Collectors.toMap(SchemaElement::getName, a -> {
                    Set<SchemaElement> result = new HashSet<>();
                    result.add(a);
                    return result;
                }, (k1, k2) -> {
                    k1.addAll(k2);
                    return k1;
                }));
    }

    private void addToSchemaMapInfo(Map<String, Set<SchemaElement>> mapResultRowSet, SchemaMapInfo schemaMap,
            SchemaElementType schemaElementType) {
        if (Objects.isNull(mapResultRowSet) || mapResultRowSet.size() <= 0) {
            return;
        }
        MapperHelper mapperHelper = ContextUtils.getBean(MapperHelper.class);

        for (Map.Entry<String, Set<SchemaElement>> entry : mapResultRowSet.entrySet()) {
            String detectWord = entry.getKey();
            Set<SchemaElement> schemaElements = entry.getValue();
            for (SchemaElement schemaElement : schemaElements) {

                List<SchemaElementMatch> elements = schemaMap.getMatchedElements(schemaElement.getDomain());
                if (CollectionUtils.isEmpty(elements)) {
                    elements = new ArrayList<>();
                    schemaMap.setMatchedElements(schemaElement.getDomain(), elements);
                }
                Set<Long> regElementSet = elements.stream()
                        .filter(elementMatch -> schemaElementType.equals(elementMatch.getElement().getType()))
                        .map(elementMatch -> elementMatch.getElement().getId())
                        .collect(Collectors.toSet());

                if (regElementSet.contains(schemaElement.getId())) {
                    continue;
                }
                SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                        .element(schemaElement)
                        .word(schemaElement.getName())
                        .detectWord(detectWord)
                        .frequency(10000L)
                        .similarity(mapperHelper.getSimilarity(detectWord, schemaElement.getName()))
                        .build();
                log.info("schemaElementType:{},add to schema, elementMatch {}", schemaElementType, schemaElementMatch);
                elements.add(schemaElementMatch);
            }
        }
    }

}
