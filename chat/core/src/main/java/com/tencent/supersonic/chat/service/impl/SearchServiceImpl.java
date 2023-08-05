package com.tencent.supersonic.chat.service.impl;

import com.google.common.collect.Lists;
import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.SearchResult;
import com.tencent.supersonic.chat.mapper.DomainInfoStat;
import com.tencent.supersonic.chat.mapper.DomainWithSemanticType;
import com.tencent.supersonic.chat.mapper.MatchText;
import com.tencent.supersonic.chat.mapper.SearchMatchStrategy;
import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.chat.service.SearchService;
import com.tencent.supersonic.chat.utils.NatureHelper;
import com.tencent.supersonic.knowledge.dictionary.DictWord;
import com.tencent.supersonic.knowledge.dictionary.MapResult;
import com.tencent.supersonic.knowledge.dictionary.DictWordType;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.knowledge.utils.HanlpHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * search service impl
 */
@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    private static final int RESULT_SIZE = 10;
    @Autowired
    private SchemaService schemaService;
    @Autowired
    private ChatService chatService;
    @Autowired
    private SearchMatchStrategy searchMatchStrategy;

    @Override
    public List<SearchResult> search(QueryReq queryCtx) {
        String queryText = queryCtx.getQueryText();
        // 1.get meta info
        SemanticSchema semanticSchemaDb = schemaService.getSemanticSchema();
        List<SchemaElement> metricsDb = semanticSchemaDb.getMetrics();
        final Map<Long, String> domainToName = semanticSchemaDb.getDomainIdToName();

        // 2.detect by segment
        List<Term> originals = HanlpHelper.getTerms(queryText);
        Map<MatchText, List<MapResult>> regTextMap = searchMatchStrategy.match(queryText, originals,
                queryCtx.getDomainId());
        regTextMap.entrySet().stream().forEach(m -> HanlpHelper.transLetterOriginal(m.getValue()));

        // 3.get the most matching data
        Optional<Entry<MatchText, List<MapResult>>> mostSimilarSearchResult = regTextMap.entrySet()
                .stream()
                .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
                .reduce((entry1, entry2) ->
                        entry1.getKey().getDetectSegment().length() >= entry2.getKey().getDetectSegment().length()
                                ? entry1 : entry2);
        log.debug("mostSimilarSearchResult:{}", mostSimilarSearchResult);

        // 4.optimize the results after the query
        if (!mostSimilarSearchResult.isPresent()) {
            return Lists.newArrayList();
        }
        Map.Entry<MatchText, List<MapResult>> searchTextEntry = mostSimilarSearchResult.get();
        log.info("searchTextEntry:{},queryCtx:{}", searchTextEntry, queryCtx);

        Set<SearchResult> searchResults = new LinkedHashSet();
        DomainInfoStat domainStat = NatureHelper.getDomainStat(originals);

        List<Long> possibleDomains = getPossibleDomains(queryCtx, originals, domainStat, queryCtx.getDomainId());

        // 4.1 priority dimension metric
        boolean existMetricAndDimension = searchMetricAndDimension(new HashSet<>(possibleDomains), domainToName,
                searchTextEntry, searchResults);

        // 4.2 process based on dimension values
        MatchText matchText = searchTextEntry.getKey();
        Map<String, String> natureToNameMap = getNatureToNameMap(searchTextEntry, new HashSet<>(possibleDomains));
        log.debug("possibleDomains:{},natureToNameMap:{}", possibleDomains, natureToNameMap);

        for (Map.Entry<String, String> natureToNameEntry : natureToNameMap.entrySet()) {

            Set<SearchResult> searchResultSet = searchDimensionValue(metricsDb, domainToName,
                    domainStat.getMetricDomainCount(), existMetricAndDimension,
                    matchText, natureToNameMap, natureToNameEntry, queryCtx.getQueryFilters());

            searchResults.addAll(searchResultSet);
        }
        return searchResults.stream().limit(RESULT_SIZE).collect(Collectors.toList());
    }

    private List<Long> getPossibleDomains(QueryReq queryCtx, List<Term> originals,
                                          DomainInfoStat domainStat, Long webDomainId) {

        if (Objects.nonNull(webDomainId) && webDomainId > 0) {
            List<Long> result = new ArrayList<>();
            result.add(webDomainId);
            return result;
        }

        List<Long> possibleDomains = NatureHelper.selectPossibleDomains(originals);

        Long contextDomain = chatService.getContextDomain(queryCtx.getChatId());

        log.debug("possibleDomains:{},domainStat:{},contextDomain:{}", possibleDomains, domainStat, contextDomain);

        // If nothing is recognized or only metric are present, then add the contextDomain.
        if (nothingOrOnlyMetric(domainStat) && effectiveDomain(contextDomain)) {
            List<Long> result = new ArrayList<>();
            result.add(contextDomain);
            return result;
        }
        return possibleDomains;
    }

    private boolean nothingOrOnlyMetric(DomainInfoStat domainStat) {
        return domainStat.getMetricDomainCount() >= 0 && domainStat.getDimensionDomainCount() <= 0
                && domainStat.getDimensionValueDomainCount() <= 0 && domainStat.getDomainCount() <= 0;
    }

    private boolean effectiveDomain(Long contextDomain) {
        return Objects.nonNull(contextDomain) && contextDomain > 0;
    }

    private Set<SearchResult> searchDimensionValue(List<SchemaElement> metricsDb,
            Map<Long, String> domainToName,
            long metricDomainCount,
            boolean existMetricAndDimension,
            MatchText matchText,
            Map<String, String> natureToNameMap,
            Map.Entry<String, String> natureToNameEntry,
            QueryFilters queryFilters) {

        Set<SearchResult> searchResults = new LinkedHashSet();
        String nature = natureToNameEntry.getKey();
        String wordName = natureToNameEntry.getValue();

        Long domainId = NatureHelper.getDomainId(nature);
        SchemaElementType schemaElementType = NatureHelper.convertToElementType(nature);

        if (SchemaElementType.ENTITY.equals(schemaElementType)) {
            return searchResults;
        }
        // If there are no metric/dimension, complete the  metric information
        SearchResult searchResult = SearchResult.builder()
                .domainId(domainId)
                .domainName(domainToName.get(domainId))
                .recommend(matchText.getRegText() + wordName)
                .schemaElementType(schemaElementType)
                .subRecommend(wordName)
                .build();
        if (metricDomainCount <= 0 && !existMetricAndDimension) {
            if (filterByQueryFilter(wordName, queryFilters)) {
                return searchResults;
            }
            searchResults.add(searchResult);
            int metricSize = getMetricSize(natureToNameMap);
            List<String> metrics = filerMetricsByDomain(metricsDb, domainId, metricSize);

            for (String metric : metrics) {
                SearchResult result = SearchResult.builder()
                        .domainId(domainId)
                        .domainName(domainToName.get(domainId))
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

    protected List<String> filerMetricsByDomain(List<SchemaElement> metricsDb, Long domain, int metricSize) {
        if (CollectionUtils.isEmpty(metricsDb)) {
            return Lists.newArrayList();
        }
        return metricsDb.stream()
                .filter(mapDO -> Objects.nonNull(mapDO) && domain.equals(mapDO.getDomain()))
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
    private Map<String, String> getNatureToNameMap(Map.Entry<MatchText, List<MapResult>> recommendTextListEntry,
            Set<Long> possibleDomains) {
        List<MapResult> recommendValues = recommendTextListEntry.getValue();
        return recommendValues.stream()
                .flatMap(entry -> entry.getNatures().stream()
                        .filter(nature -> {
                            if (CollectionUtils.isEmpty(possibleDomains)) {
                                return true;
                            }
                            Long domain = NatureHelper.getDomainId(nature);
                            return possibleDomains.contains(domain);
                        })
                        .map(nature -> {
                                    DictWord posDO = new DictWord();
                                    posDO.setWord(entry.getName());
                                    posDO.setNature(nature);
                                    return posDO;
                                }
                        )).sorted(Comparator.comparingInt(a -> a.getWord().length()))
                .collect(Collectors.toMap(DictWord::getNature, DictWord::getWord, (value1, value2) -> value1,
                        LinkedHashMap::new));
    }

    private boolean searchMetricAndDimension(Set<Long> possibleDomains, Map<Long, String> domainToName,
            Map.Entry<MatchText, List<MapResult>> searchTextEntry, Set<SearchResult> searchResults) {
        boolean existMetric = false;

        MatchText matchText = searchTextEntry.getKey();
        List<MapResult> mapResults = searchTextEntry.getValue();

        for (MapResult mapResult : mapResults) {

            List<DomainWithSemanticType> dimensionMetricClassIds = mapResult.getNatures().stream()
                    .map(nature -> new DomainWithSemanticType(NatureHelper.getDomainId(nature),
                            NatureHelper.convertToElementType(nature)))
                    .filter(entry -> matchCondition(entry, possibleDomains)).collect(Collectors.toList());

            if (CollectionUtils.isEmpty(dimensionMetricClassIds)) {
                continue;
            }
            for (DomainWithSemanticType domainWithSemanticType : dimensionMetricClassIds) {
                existMetric = true;
                Long domainId = domainWithSemanticType.getDomain();
                SchemaElementType semanticType = domainWithSemanticType.getSemanticType();

                SearchResult searchResult = SearchResult.builder()
                        .domainId(domainId)
                        .domainName(domainToName.get(domainId))
                        .recommend(matchText.getRegText() + mapResult.getName())
                        .subRecommend(mapResult.getName())
                        .schemaElementType(semanticType)
                        .build();

                searchResults.add(searchResult);
            }
            log.info("parseResult:{},dimensionMetricClassIds:{},possibleDomains:{}", mapResult, dimensionMetricClassIds,
                    possibleDomains);
        }
        return existMetric;
    }

    private boolean matchCondition(DomainWithSemanticType entry, Set<Long> possibleDomains) {
        if (!(SchemaElementType.METRIC.equals(entry.getSemanticType()) || SchemaElementType.DIMENSION.equals(
                entry.getSemanticType()))) {
            return false;
        }

        if (CollectionUtils.isEmpty(possibleDomains)) {
            return true;
        }
        return possibleDomains.contains(entry.getDomain());
    }
}
