package com.tencent.supersonic.headless.server.facade.service.impl;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;
import com.tencent.supersonic.headless.chat.QueryContext;
import com.tencent.supersonic.headless.chat.knowledge.DataSetInfoStat;
import com.tencent.supersonic.headless.chat.knowledge.DictWord;
import com.tencent.supersonic.headless.chat.knowledge.HanlpMapResult;
import com.tencent.supersonic.headless.chat.knowledge.KnowledgeBaseService;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import com.tencent.supersonic.headless.chat.knowledge.helper.NatureHelper;
import com.tencent.supersonic.headless.chat.mapper.MatchText;
import com.tencent.supersonic.headless.chat.mapper.ModelWithSemanticType;
import com.tencent.supersonic.headless.chat.mapper.SearchMatchStrategy;
import com.tencent.supersonic.headless.server.facade.service.RetrieveService;
import com.tencent.supersonic.headless.server.web.service.ChatContextService;
import com.tencent.supersonic.headless.server.web.service.DataSetService;
import com.tencent.supersonic.headless.server.web.service.SchemaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private ChatContextService chatContextService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private SearchMatchStrategy searchMatchStrategy;
    @Override
    public List<SearchResult> retrieve(QueryReq queryReq) {

        String queryText = queryReq.getQueryText();
        // 1.get meta info
        SemanticSchema semanticSchemaDb = schemaService.getSemanticSchema();
        List<SchemaElement> metricsDb = semanticSchemaDb.getMetrics();
        final Map<Long, String> dataSetIdToName = semanticSchemaDb.getDataSetIdToName();
        Map<Long, List<Long>> modelIdToDataSetIds =
                dataSetService.getModelIdToDataSetIds(new ArrayList<>(dataSetIdToName.keySet()), User.getFakeUser());
        // 2.detect by segment
        List<S2Term> originals = knowledgeBaseService.getTerms(queryText, modelIdToDataSetIds);
        log.info("hanlp parse result: {}", originals);
        Set<Long> dataSetIds = queryReq.getDataSetIds();

        QueryContext queryContext = new QueryContext();
        BeanUtils.copyProperties(queryReq, queryContext);
        queryContext.setModelIdToDataSetIds(dataSetService.getModelIdToDataSetIds());

        Map<MatchText, List<HanlpMapResult>> regTextMap =
                searchMatchStrategy.match(queryContext, originals, dataSetIds);

        regTextMap.entrySet().stream().forEach(m -> HanlpHelper.transLetterOriginal(m.getValue()));

        // 3.get the most matching data
        Optional<Map.Entry<MatchText, List<HanlpMapResult>>> mostSimilarSearchResult = regTextMap.entrySet()
                .stream()
                .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
                .reduce((entry1, entry2) ->
                        entry1.getKey().getDetectSegment().length() >= entry2.getKey().getDetectSegment().length()
                                ? entry1 : entry2);

        // 4.optimize the results after the query
        if (!mostSimilarSearchResult.isPresent()) {
            return Lists.newArrayList();
        }
        Map.Entry<MatchText, List<HanlpMapResult>> searchTextEntry = mostSimilarSearchResult.get();
        log.info("searchTextEntry:{},queryReq:{}", searchTextEntry, queryReq);

        Set<SearchResult> searchResults = new LinkedHashSet();
        DataSetInfoStat dataSetInfoStat = NatureHelper.getDataSetStat(originals);

        List<Long> possibleDataSets = getPossibleDataSets(queryReq, originals, dataSetInfoStat, dataSetIds);

        // 5.1 priority dimension metric
        boolean existMetricAndDimension = searchMetricAndDimension(new HashSet<>(possibleDataSets), dataSetIdToName,
                searchTextEntry, searchResults);

        // 5.2 process based on dimension values
        MatchText matchText = searchTextEntry.getKey();
        Map<String, String> natureToNameMap = getNatureToNameMap(searchTextEntry, new HashSet<>(possibleDataSets));
        log.debug("possibleDataSets:{},natureToNameMap:{}", possibleDataSets, natureToNameMap);

        for (Map.Entry<String, String> natureToNameEntry : natureToNameMap.entrySet()) {

            Set<SearchResult> searchResultSet = searchDimensionValue(metricsDb, dataSetIdToName,
                    dataSetInfoStat.getMetricDataSetCount(), existMetricAndDimension,
                    matchText, natureToNameMap, natureToNameEntry, queryReq.getQueryFilters());

            searchResults.addAll(searchResultSet);
        }
        return searchResults.stream().limit(RESULT_SIZE).collect(Collectors.toList());
    }

    private List<Long> getPossibleDataSets(QueryReq queryCtx, List<S2Term> originals,
                                           DataSetInfoStat dataSetInfoStat, Set<Long> dataSetIds) {
        if (CollectionUtils.isNotEmpty(dataSetIds)) {
            return new ArrayList<>(dataSetIds);
        }

        List<Long> possibleDataSets = NatureHelper.selectPossibleDataSets(originals);

        Long contextModel = chatContextService.getContextModel(queryCtx.getChatId());

        log.debug("possibleDataSets:{},dataSetInfoStat:{},contextModel:{}",
                possibleDataSets, dataSetInfoStat, contextModel);

        // If nothing is recognized or only metric are present, then add the contextModel.
        if (nothingOrOnlyMetric(dataSetInfoStat)) {
            return Lists.newArrayList(contextModel);
        }
        return possibleDataSets;
    }

    private boolean nothingOrOnlyMetric(DataSetInfoStat modelStat) {
        return modelStat.getMetricDataSetCount() >= 0 && modelStat.getDimensionDataSetCount() <= 0
                && modelStat.getDimensionValueDataSetCount() <= 0 && modelStat.getDataSetCount() <= 0;
    }

    private Set<SearchResult> searchDimensionValue(List<SchemaElement> metricsDb,
                                                   Map<Long, String> modelToName,
                                                   long metricModelCount,
                                                   boolean existMetricAndDimension,
                                                   MatchText matchText,
                                                   Map<String, String> natureToNameMap,
                                                   Map.Entry<String, String> natureToNameEntry,
                                                   QueryFilters queryFilters) {

        Set<SearchResult> searchResults = new LinkedHashSet();
        String nature = natureToNameEntry.getKey();
        String wordName = natureToNameEntry.getValue();

        Long modelId = NatureHelper.getDataSetId(nature);
        SchemaElementType schemaElementType = NatureHelper.convertToElementType(nature);

        if (SchemaElementType.ENTITY.equals(schemaElementType)) {
            return searchResults;
        }
        // If there are no metric/dimension, complete the  metric information
        SearchResult searchResult = SearchResult.builder()
                .modelId(modelId)
                .modelName(modelToName.get(modelId))
                .recommend(matchText.getRegText() + wordName)
                .schemaElementType(schemaElementType)
                .subRecommend(wordName)
                .build();

        if (metricModelCount <= 0 && !existMetricAndDimension) {
            if (filterByQueryFilter(wordName, queryFilters)) {
                return searchResults;
            }
            searchResults.add(searchResult);
            int metricSize = getMetricSize(natureToNameMap);
            List<String> metrics = filerMetricsByModel(metricsDb, modelId, metricSize * 3)
                    .stream()
                    .limit(metricSize).collect(Collectors.toList());

            for (String metric : metrics) {
                SearchResult result = SearchResult.builder()
                        .modelId(modelId)
                        .modelName(modelToName.get(modelId))
                        .recommend(matchText.getRegText() + wordName + DictWordType.SPACE + metric)
                        .subRecommend(wordName + DictWordType.SPACE + metric)
                        .isComplete(false)
                        .build();
                searchResults.add(result);
            }
        } else {
            searchResults.add(searchResult);
        }
        return searchResults;
    }

    private int getMetricSize(Map<String, String> natureToNameMap) {
        int metricSize = RESULT_SIZE / (natureToNameMap.entrySet().size());
        if (metricSize <= 1) {
            metricSize = 1;
        }
        return metricSize;
    }

    private boolean filterByQueryFilter(String wordName, QueryFilters queryFilters) {
        if (queryFilters == null || CollectionUtils.isEmpty(queryFilters.getFilters())) {
            return false;
        }
        List<QueryFilter> filters = queryFilters.getFilters();
        for (QueryFilter filter : filters) {
            if (wordName.equalsIgnoreCase(String.valueOf(filter.getValue()))) {
                return false;
            }
        }
        return true;
    }

    protected List<String> filerMetricsByModel(List<SchemaElement> metricsDb, Long model, int metricSize) {
        if (CollectionUtils.isEmpty(metricsDb)) {
            return Lists.newArrayList();
        }
        return metricsDb.stream()
                .filter(mapDO -> Objects.nonNull(mapDO) && model.equals(mapDO.getDataSet()))
                .sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                .flatMap(entry -> {
                    List<String> result = new ArrayList<>();
                    result.add(entry.getName());
                    return result.stream();
                })
                .limit(metricSize).collect(Collectors.toList());
    }

    /***
     * convert nature to name
     * @param recommendTextListEntry
     * @return
     */
    private Map<String, String> getNatureToNameMap(Map.Entry<MatchText, List<HanlpMapResult>> recommendTextListEntry,
                                                   Set<Long> possibleModels) {
        List<HanlpMapResult> recommendValues = recommendTextListEntry.getValue();
        return recommendValues.stream()
                .flatMap(entry -> entry.getNatures().stream()
                        .filter(nature -> {
                            if (CollectionUtils.isEmpty(possibleModels)) {
                                return true;
                            }
                            Long model = NatureHelper.getDataSetId(nature);
                            return possibleModels.contains(model);
                        })
                        .map(nature -> {
                            DictWord posDO = new DictWord();
                            posDO.setWord(entry.getName());
                            posDO.setNature(nature);
                            return posDO;
                        })).sorted(Comparator.comparingInt(a -> a.getWord().length()))
                .collect(Collectors.toMap(DictWord::getNature, DictWord::getWord, (value1, value2) -> value1,
                        LinkedHashMap::new));
    }

    private boolean searchMetricAndDimension(Set<Long> possibleDataSets, Map<Long, String> modelToName,
                                             Map.Entry<MatchText, List<HanlpMapResult>> searchTextEntry,
                                             Set<SearchResult> searchResults) {
        boolean existMetric = false;
        log.info("searchMetricAndDimension searchTextEntry:{}", searchTextEntry);
        MatchText matchText = searchTextEntry.getKey();
        List<HanlpMapResult> hanlpMapResults = searchTextEntry.getValue();

        for (HanlpMapResult hanlpMapResult : hanlpMapResults) {

            List<ModelWithSemanticType> dimensionMetricClassIds = hanlpMapResult.getNatures().stream()
                    .map(nature -> new ModelWithSemanticType(NatureHelper.getDataSetId(nature),
                            NatureHelper.convertToElementType(nature)))
                    .filter(entry -> matchCondition(entry, possibleDataSets)).collect(Collectors.toList());

            if (CollectionUtils.isEmpty(dimensionMetricClassIds)) {
                continue;
            }
            for (ModelWithSemanticType modelWithSemanticType : dimensionMetricClassIds) {
                existMetric = true;
                Long modelId = modelWithSemanticType.getModel();
                SchemaElementType schemaElementType = modelWithSemanticType.getSchemaElementType();
                SearchResult searchResult = SearchResult.builder()
                        .modelId(modelId)
                        .modelName(modelToName.get(modelId))
                        .recommend(matchText.getRegText() + hanlpMapResult.getName())
                        .subRecommend(hanlpMapResult.getName())
                        .schemaElementType(schemaElementType)
                        .build();
                //visibility to filter  metrics
                searchResults.add(searchResult);
            }
            log.info("parseResult:{},dimensionMetricClassIds:{},possibleDataSets:{}", hanlpMapResult,
                    dimensionMetricClassIds, possibleDataSets);
        }
        log.info("searchMetricAndDimension searchResults:{}", searchResults);
        return existMetric;
    }

    private boolean matchCondition(ModelWithSemanticType entry, Set<Long> possibleDataSets) {
        if (!(SchemaElementType.METRIC.equals(entry.getSchemaElementType()) || SchemaElementType.DIMENSION.equals(
                entry.getSchemaElementType()))) {
            return false;
        }

        if (CollectionUtils.isEmpty(possibleDataSets)) {
            return true;
        }
        return possibleDataSets.contains(entry.getModel());
    }

}
