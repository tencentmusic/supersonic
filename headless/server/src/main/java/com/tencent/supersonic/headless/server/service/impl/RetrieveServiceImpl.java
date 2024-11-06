package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.DataSetInfoStat;
import com.tencent.supersonic.headless.chat.knowledge.DictWord;
import com.tencent.supersonic.headless.chat.knowledge.HanlpMapResult;
import com.tencent.supersonic.headless.chat.knowledge.KnowledgeBaseService;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import com.tencent.supersonic.headless.chat.knowledge.helper.NatureHelper;
import com.tencent.supersonic.headless.chat.mapper.DataSetWithSemanticType;
import com.tencent.supersonic.headless.chat.mapper.MatchText;
import com.tencent.supersonic.headless.chat.mapper.SearchMatchStrategy;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.RetrieveService;
import com.tencent.supersonic.headless.server.service.SchemaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RetrieveServiceImpl implements RetrieveService {

    private static final int RESULT_SIZE = 10;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private SearchMatchStrategy searchMatchStrategy;

    @Override
    public List<SearchResult> retrieve(QueryNLReq queryNLReq) {
        String queryText = queryNLReq.getQueryText();

        // 1. Get meta info
        SemanticSchema semanticSchemaDb =
                schemaService.getSemanticSchema(queryNLReq.getDataSetIds());
        Map<Long, String> dataSetIdToName = semanticSchemaDb.getDataSetIdToName();
        Map<Long, List<Long>> modelIdToDataSetIds = dataSetService.getModelIdToDataSetIds(
                new ArrayList<>(dataSetIdToName.keySet()), User.getDefaultUser());

        // 2. Detect by segment
        List<S2Term> originals = knowledgeBaseService.getTerms(queryText, modelIdToDataSetIds);
        log.debug("originals terms: {}", originals);
        Set<Long> dataSetIds = queryNLReq.getDataSetIds();

        ChatQueryContext chatQueryContext = new ChatQueryContext(queryNLReq);
        chatQueryContext.setModelIdToDataSetIds(dataSetService.getModelIdToDataSetIds());

        Map<MatchText, List<HanlpMapResult>> regTextMap =
                searchMatchStrategy.match(chatQueryContext, originals, dataSetIds);
        regTextMap.values().forEach(HanlpHelper::transLetterOriginal);

        // 3. Get the most matching data
        Optional<Map.Entry<MatchText, List<HanlpMapResult>>> mostSimilarSearchResult = regTextMap
                .entrySet().stream().filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
                .max(Comparator.comparingInt(entry -> entry.getKey().getDetectSegment().length()));

        if (!mostSimilarSearchResult.isPresent()) {
            return Collections.emptyList();
        }

        Map.Entry<MatchText, List<HanlpMapResult>> searchTextEntry = mostSimilarSearchResult.get();
        log.debug("searchTextEntry:{},queryNLReq:{}", searchTextEntry, queryNLReq);


        DataSetInfoStat dataSetInfoStat = NatureHelper.getDataSetStat(originals);
        List<Long> possibleDataSets = getPossibleDataSets(queryNLReq, originals, dataSetIds);

        // 5.1 Priority dimension metric
        Set<SearchResult> searchResults = searchMetricAndDimension(new HashSet<>(possibleDataSets),
                dataSetIdToName, searchTextEntry);
        boolean existMetricAndDimension = CollectionUtils.isNotEmpty(searchResults);

        // 5.2 Process based on dimension values
        MatchText matchText = searchTextEntry.getKey();
        Map<String, String> natureToNameMap =
                getNatureToNameMap(searchTextEntry, new HashSet<>(possibleDataSets));
        log.debug("possibleDataSets:{},natureToNameMap:{}", possibleDataSets, natureToNameMap);

        for (Map.Entry<String, String> natureToNameEntry : natureToNameMap.entrySet()) {
            Set<SearchResult> results = searchDimensionValue(semanticSchemaDb,
                    dataSetInfoStat.getMetricDataSetCount(), existMetricAndDimension, matchText,
                    natureToNameMap, natureToNameEntry, queryNLReq.getQueryFilters());
            searchResults.addAll(results);
        }

        return searchResults.stream().limit(RESULT_SIZE).collect(Collectors.toList());
    }

    private List<Long> getPossibleDataSets(QueryNLReq queryReq, List<S2Term> originals,
            Set<Long> dataSetIds) {
        if (CollectionUtils.isNotEmpty(dataSetIds)) {
            return new ArrayList<>(dataSetIds);
        }

        List<Long> possibleDataSets = NatureHelper.selectPossibleDataSets(originals);
        if (possibleDataSets.isEmpty()) {
            if (Objects.nonNull(queryReq.getContextParseInfo())) {
                possibleDataSets.add(queryReq.getContextParseInfo().getDataSetId());
            }
        }

        return possibleDataSets;
    }

    private Set<SearchResult> searchDimensionValue(SemanticSchema semanticSchemaDb,
            long metricModelCount, boolean existMetricAndDimension, MatchText matchText,
            Map<String, String> natureToNameMap, Map.Entry<String, String> natureToNameEntry,
            QueryFilters queryFilters) {
        List<SchemaElement> metricsDb = semanticSchemaDb.getMetrics();
        Map<Long, String> dataSetIdToName = semanticSchemaDb.getDataSetIdToName();

        Set<SearchResult> searchResults = new LinkedHashSet<>();
        String nature = natureToNameEntry.getKey();
        String wordName = natureToNameEntry.getValue();

        Long dataSetId = NatureHelper.getDataSetId(nature);
        SchemaElementType schemaElementType = NatureHelper.convertToElementType(nature);

        // Create a base search result
        SearchResult baseSearchResult = createBaseSearchResult(dataSetId, dataSetIdToName,
                matchText, wordName, schemaElementType);

        // If there are no metrics or dimensions, complete the metric information
        if (shouldCompleteMetricInfo(metricModelCount, existMetricAndDimension)) {
            if (filterByQueryFilter(wordName, queryFilters)) {
                return searchResults;
            }
            searchResults.add(baseSearchResult);

            int metricSize = calculateMetricSize(natureToNameMap);
            List<String> metrics = getFilteredMetrics(metricsDb, dataSetId, metricSize);

            for (String metric : metrics) {
                SearchResult metricSearchResult = createMetricSearchResult(dataSetId,
                        dataSetIdToName, matchText, wordName, metric);
                searchResults.add(metricSearchResult);
            }
        } else {
            searchResults.add(baseSearchResult);
        }

        return searchResults;
    }

    private SearchResult createBaseSearchResult(Long dataSetId, Map<Long, String> dataSetIdToName,
            MatchText matchText, String wordName, SchemaElementType schemaElementType) {
        return SearchResult.builder().dataSetId(dataSetId)
                .dataSetName(dataSetIdToName.get(dataSetId))
                .recommend(matchText.getRegText() + wordName).schemaElementType(schemaElementType)
                .subRecommend(wordName).build();
    }

    private boolean shouldCompleteMetricInfo(long metricModelCount,
            boolean existMetricAndDimension) {
        return metricModelCount <= 0 && !existMetricAndDimension;
    }

    private int calculateMetricSize(Map<String, String> natureToNameMap) {
        int metricSize = RESULT_SIZE / natureToNameMap.size();
        return Math.max(metricSize, 1);
    }

    private List<String> getFilteredMetrics(List<SchemaElement> metricsDb, Long modelId,
            int metricSize) {
        return metricsDb.stream()
                .filter(mapDO -> Objects.nonNull(mapDO) && modelId.equals(mapDO.getDataSetId()))
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .map(SchemaElement::getName).limit(metricSize).collect(Collectors.toList());
    }

    private SearchResult createMetricSearchResult(Long modelId, Map<Long, String> modelToName,
            MatchText matchText, String wordName, String metric) {
        return SearchResult.builder().dataSetId(modelId).dataSetName(modelToName.get(modelId))
                .recommend(matchText.getRegText() + wordName + DictWordType.SPACE + metric)
                .subRecommend(wordName + DictWordType.SPACE + metric).isComplete(false).build();
    }

    private boolean filterByQueryFilter(String wordName, QueryFilters queryFilters) {
        if (queryFilters == null || CollectionUtils.isEmpty(queryFilters.getFilters())) {
            return false;
        }
        return queryFilters.getFilters().stream()
                .noneMatch(filter -> wordName.equalsIgnoreCase(String.valueOf(filter.getValue())));
    }

    /**
     * * convert nature to name
     *
     * @param recommendTextListEntry
     * @return
     */
    private Map<String, String> getNatureToNameMap(
            Map.Entry<MatchText, List<HanlpMapResult>> recommendTextListEntry,
            Set<Long> possibleModels) {

        List<HanlpMapResult> recommendValues = recommendTextListEntry.getValue();

        return recommendValues.stream().flatMap(entry -> {
            List<String> filteredNatures = entry.getNatures().stream()
                    .filter(nature -> isNatureValid(nature, possibleModels))
                    .collect(Collectors.toList());

            return filteredNatures.stream()
                    .map(nature -> DictWord.builder().word(entry.getName()).nature(nature).build());
        }).sorted(Comparator.comparingInt(dictWord -> dictWord.getWord().length()))
                .collect(Collectors.toMap(DictWord::getNature, DictWord::getWord,
                        (value1, value2) -> value1, LinkedHashMap::new));
    }

    private boolean isNatureValid(String nature, Set<Long> possibleModels) {
        if (CollectionUtils.isEmpty(possibleModels)) {
            return true;
        }
        Long model = NatureHelper.getDataSetId(nature);
        return possibleModels.contains(model);
    }

    private Set<SearchResult> searchMetricAndDimension(Set<Long> possibleDataSets,
            Map<Long, String> dataSetIdToName,
            Map.Entry<MatchText, List<HanlpMapResult>> searchTextEntry) {

        Set<SearchResult> searchResults = new LinkedHashSet<>();
        log.debug("searchMetricAndDimension searchTextEntry:{}", searchTextEntry);

        MatchText matchText = searchTextEntry.getKey();
        List<HanlpMapResult> hanlpMapResults = searchTextEntry.getValue();

        for (HanlpMapResult hanlpMapResult : hanlpMapResults) {
            List<DataSetWithSemanticType> dimensionMetricDataSetIds = hanlpMapResult.getNatures()
                    .stream()
                    .map(nature -> new DataSetWithSemanticType(NatureHelper.getDataSetId(nature),
                            NatureHelper.convertToElementType(nature)))
                    .filter(entry -> matchCondition(entry, possibleDataSets))
                    .collect(Collectors.toList());

            if (CollectionUtils.isEmpty(dimensionMetricDataSetIds)) {
                continue;
            }
            for (DataSetWithSemanticType dataSetWithSemanticType : dimensionMetricDataSetIds) {
                Long dataSetId = dataSetWithSemanticType.getDataSetId();
                SchemaElementType schemaElementType =
                        dataSetWithSemanticType.getSchemaElementType();
                String modelName = dataSetIdToName.get(dataSetId);
                String recommendText = matchText.getRegText() + hanlpMapResult.getName();
                String subRecommendText = hanlpMapResult.getName();

                SearchResult searchResult =
                        SearchResult.builder().dataSetId(dataSetId).dataSetName(modelName)
                                .recommend(recommendText).subRecommend(subRecommendText)
                                .schemaElementType(schemaElementType).build();

                searchResults.add(searchResult);
            }
        }
        log.info("searchMetricAndDimension searchResults:{}", searchResults);
        return searchResults;
    }

    private boolean matchCondition(DataSetWithSemanticType entry, Set<Long> possibleDataSets) {
        if (!(SchemaElementType.METRIC.equals(entry.getSchemaElementType())
                || SchemaElementType.DIMENSION.equals(entry.getSchemaElementType()))) {
            return false;
        }

        if (CollectionUtils.isEmpty(possibleDataSets)) {
            return true;
        }
        return possibleDataSets.contains(entry.getDataSetId());
    }
}
