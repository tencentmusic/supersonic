package com.tencent.supersonic.headless.server.service.impl;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import com.tencent.supersonic.headless.api.pojo.request.QueryMapReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.DataSetMapInfo;
import com.tencent.supersonic.headless.api.pojo.response.DataSetResp;
import com.tencent.supersonic.headless.api.pojo.response.MapInfoResp;
import com.tencent.supersonic.headless.api.pojo.response.MapResp;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.api.pojo.response.SearchResult;
import com.tencent.supersonic.headless.chat.QueryContext;
import com.tencent.supersonic.headless.chat.knowledge.DataSetInfoStat;
import com.tencent.supersonic.headless.chat.knowledge.DictWord;
import com.tencent.supersonic.headless.chat.knowledge.HanlpMapResult;
import com.tencent.supersonic.headless.chat.knowledge.KnowledgeBaseService;
import com.tencent.supersonic.headless.chat.knowledge.builder.BaseWordBuilder;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import com.tencent.supersonic.headless.chat.knowledge.helper.NatureHelper;
import com.tencent.supersonic.headless.chat.mapper.MatchText;
import com.tencent.supersonic.headless.chat.mapper.ModelWithSemanticType;
import com.tencent.supersonic.headless.chat.mapper.SearchMatchStrategy;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.ChatContextService;
import com.tencent.supersonic.headless.server.service.ChatQueryService;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.RetrieveService;
import com.tencent.supersonic.headless.server.service.SemanticLayerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RetrieveServiceImpl implements RetrieveService {

    private static final int RESULT_SIZE = 10;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ChatQueryService chatQueryService;

    @Autowired
    private ChatContextService chatContextService;

    @Autowired
    private SemanticLayerService semanticLayerService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private SearchMatchStrategy searchMatchStrategy;

    @Override
    public MapInfoResp map(QueryMapReq queryMapReq) {

        QueryReq queryReq = new QueryReq();
        BeanUtils.copyProperties(queryMapReq, queryReq);
        List<DataSetResp> dataSets = dataSetService.getDataSets(queryMapReq.getDataSetNames(), queryMapReq.getUser());

        Set<Long> dataSetIds = dataSets.stream().map(SchemaItem::getId).collect(Collectors.toSet());
        queryReq.setDataSetIds(dataSetIds);
        MapResp mapResp = chatQueryService.performMapping(queryReq);
        dataSetIds.retainAll(mapResp.getMapInfo().getDataSetElementMatches().keySet());
        return convert(mapResp, queryMapReq.getTopN(), dataSetIds);
    }

    @Override
    public List<SearchResult> search(QueryReq queryReq) {

        String queryText = queryReq.getQueryText();
        // 1.get meta info
        SemanticSchema semanticSchemaDb = semanticLayerService.getSemanticSchema();
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

    private MapInfoResp convert(MapResp mapResp, Integer topN, Set<Long> dataSetIds) {
        MapInfoResp mapInfoResp = new MapInfoResp();
        if (Objects.isNull(mapResp)) {
            return mapInfoResp;
        }
        BeanUtils.copyProperties(mapResp, mapInfoResp);
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setIds(new ArrayList<>(dataSetIds));
        List<DataSetResp> dataSetList = dataSetService.getDataSetList(metaFilter);
        Map<Long, DataSetResp> dataSetMap = dataSetList.stream()
                .collect(Collectors.toMap(DataSetResp::getId, d -> d));
        mapInfoResp.setDataSetMapInfo(getDataSetInfo(mapResp.getMapInfo(), dataSetMap, topN));
        mapInfoResp.setTerms(getTerms(mapResp.getMapInfo(), dataSetMap));
        return mapInfoResp;
    }

    private Map<String, DataSetMapInfo> getDataSetInfo(SchemaMapInfo mapInfo,
                                                       Map<Long, DataSetResp> dataSetMap,
                                                       Integer topN) {
        Map<String, DataSetMapInfo> map = new HashMap<>();
        Map<Long, List<SchemaElementMatch>> mapFields = getMapFields(mapInfo, dataSetMap);
        Map<Long, List<SchemaElementMatch>> topFields = getTopFields(topN, mapInfo, dataSetMap);
        for (Long dataSetId : mapInfo.getDataSetElementMatches().keySet()) {
            DataSetResp dataSetResp = dataSetMap.get(dataSetId);
            if (dataSetResp == null) {
                continue;
            }
            if (CollectionUtils.isEmpty(mapFields.get(dataSetId))) {
                continue;
            }
            DataSetMapInfo dataSetMapInfo = new DataSetMapInfo();
            dataSetMapInfo.setMapFields(mapFields.getOrDefault(dataSetId, Lists.newArrayList()));
            dataSetMapInfo.setTopFields(topFields.getOrDefault(dataSetId, Lists.newArrayList()));
            dataSetMapInfo.setName(dataSetResp.getName());
            dataSetMapInfo.setDescription(dataSetResp.getDescription());
            map.put(dataSetMapInfo.getName(), dataSetMapInfo);
        }
        return map;
    }

    private Map<Long, List<SchemaElementMatch>> getMapFields(SchemaMapInfo mapInfo,
                                                               Map<Long, DataSetResp> dataSetMap) {
        Map<Long, List<SchemaElementMatch>> result = new HashMap<>();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : mapInfo.getDataSetElementMatches().entrySet()) {
            List<SchemaElementMatch> values = entry.getValue().stream()
                    .filter(schemaElementMatch ->
                            !SchemaElementType.TERM.equals(schemaElementMatch.getElement().getType()))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(values) && dataSetMap.containsKey(entry.getKey())) {
                result.put(entry.getKey(), values);
            }
        }
        return result;
    }

    private Map<Long, List<SchemaElementMatch>> getTopFields(Integer topN,
                                                               SchemaMapInfo mapInfo,
                                                               Map<Long, DataSetResp> dataSetMap) {
        Map<Long, List<SchemaElementMatch>> result = new HashMap<>();
        if (0 == topN) {
            return result;
        }
        SemanticSchema semanticSchema = semanticLayerService.getSemanticSchema();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : mapInfo.getDataSetElementMatches().entrySet()) {
            Long dataSetId = entry.getKey();
            List<SchemaElementMatch> values = entry.getValue();
            DataSetResp dataSetResp = dataSetMap.get(dataSetId);
            if (dataSetResp == null || CollectionUtils.isEmpty(values)) {
                continue;
            }
            String dataSetName = dataSetResp.getName();
            //topN dimensions
            Set<SchemaElementMatch> dimensions = semanticSchema.getDimensions(dataSetId)
                    .stream().sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                    .limit(topN - 1).map(mergeFunction()).collect(Collectors.toSet());

            SchemaElementMatch timeDimensionMatch = getTimeDimension(dataSetId, dataSetName);
            dimensions.add(timeDimensionMatch);

            //topN metrics
            Set<SchemaElementMatch> metrics = semanticSchema.getMetrics(dataSetId)
                    .stream().sorted(Comparator.comparing(SchemaElement::getUseCnt).reversed())
                    .limit(topN).map(mergeFunction()).collect(Collectors.toSet());

            dimensions.addAll(metrics);
            result.put(dataSetId, new ArrayList<>(dimensions));
        }
        return result;
    }

    private Map<String, List<SchemaElementMatch>> getTerms(SchemaMapInfo mapInfo,
                                                           Map<Long, DataSetResp> dataSetNameMap) {
        Map<String, List<SchemaElementMatch>> termMap = new HashMap<>();
        Map<Long, List<SchemaElementMatch>> dataSetElementMatches = mapInfo.getDataSetElementMatches();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : dataSetElementMatches.entrySet()) {
            DataSetResp dataSetResp = dataSetNameMap.get(entry.getKey());
            if (dataSetResp == null) {
                continue;
            }
            List<SchemaElementMatch> terms = entry.getValue().stream().filter(schemaElementMatch
                            -> SchemaElementType.TERM.equals(schemaElementMatch.getElement().getType()))
                    .collect(Collectors.toList());
            termMap.put(dataSetResp.getName(), terms);
        }
        return termMap;
    }

    /***
     * get time dimension SchemaElementMatch
     * @param dataSetId
     * @param dataSetName
     * @return
     */
    private SchemaElementMatch getTimeDimension(Long dataSetId, String dataSetName) {
        SchemaElement element = SchemaElement.builder().dataSet(dataSetId).dataSetName(dataSetName)
                .type(SchemaElementType.DIMENSION).bizName(TimeDimensionEnum.DAY.getName()).build();

        SchemaElementMatch timeDimensionMatch = SchemaElementMatch.builder().element(element)
                .detectWord(TimeDimensionEnum.DAY.getChName()).word(TimeDimensionEnum.DAY.getChName())
                .similarity(1L).frequency(BaseWordBuilder.DEFAULT_FREQUENCY).build();

        return timeDimensionMatch;
    }

    private Function<SchemaElement, SchemaElementMatch> mergeFunction() {
        return schemaElement -> SchemaElementMatch.builder().element(schemaElement)
                .frequency(BaseWordBuilder.DEFAULT_FREQUENCY).word(schemaElement.getName()).similarity(1)
                .detectWord(schemaElement.getName()).build();
    }
}
