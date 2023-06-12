package com.tencent.supersonic.chat.application;

import com.google.common.collect.Lists;
import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SemanticLayer;
import com.tencent.supersonic.chat.application.knowledge.NatureHelper;
import com.tencent.supersonic.chat.application.mapper.SearchMatchStrategy;
import com.tencent.supersonic.chat.domain.pojo.search.DomainInfoStat;
import com.tencent.supersonic.chat.domain.pojo.search.DomainWithSemanticType;
import com.tencent.supersonic.chat.domain.pojo.search.MatchText;
import com.tencent.supersonic.chat.domain.pojo.search.SearchResult;
import com.tencent.supersonic.chat.domain.pojo.semantic.DomainInfos;
import com.tencent.supersonic.chat.domain.service.ChatService;
import com.tencent.supersonic.chat.domain.service.SearchService;
import com.tencent.supersonic.chat.domain.utils.NatureConverter;
import com.tencent.supersonic.chat.domain.utils.SchemaInfoConverter;
import com.tencent.supersonic.common.nlp.ItemDO;
import com.tencent.supersonic.common.nlp.MapResult;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.common.nlp.WordNature;
import com.tencent.supersonic.knowledge.application.online.BaseWordNature;
import com.tencent.supersonic.knowledge.infrastructure.nlp.HanlpHelper;
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
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * search service impl
 */
@Service
public class SearchServiceImpl implements SearchService {

    private final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
    @Autowired
    private SemanticLayer semanticLayer;
    @Autowired
    private ChatService chatService;

    @Autowired
    private SearchMatchStrategy searchMatchStrategy;

    private static final int RESULT_SIZE = 10;


    @Override
    public List<SearchResult> search(QueryContextReq queryCtx) {
        String queryText = queryCtx.getQueryText();
        // 1.get meta info
        DomainInfos domainInfosDb = SchemaInfoConverter.convert(semanticLayer.getDomainSchemaInfo(new ArrayList<>()));
        List<ItemDO> metricsDb = domainInfosDb.getMetrics();
        final Map<Integer, String> domainToName = domainInfosDb.getDomainToName();
        // 2.detect by segment
        List<Term> originals = HanlpHelper.getSegment().seg(queryText).stream().collect(Collectors.toList());
        Map<MatchText, List<MapResult>> regTextMap = searchMatchStrategy.matchWithMatchText(queryText, originals);

        // 3.get the most matching data
        Optional<Entry<MatchText, List<MapResult>>> mostSimilarSearchResult = regTextMap.entrySet()
                .stream()
                .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
                .reduce((entry1, entry2) ->
                        entry1.getKey().getDetectSegment().length() >= entry2.getKey().getDetectSegment().length()
                                ? entry1 : entry2);
        logger.debug("mostSimilarSearchResult:{}", mostSimilarSearchResult);
        // 4.optimize the results after the query
        if (!mostSimilarSearchResult.isPresent()) {
            logger.info("unable to find any information through search , queryCtx:{}", queryCtx);
            return Lists.newArrayList();
        }
        Map.Entry<MatchText, List<MapResult>> searchTextEntry = mostSimilarSearchResult.get();
        logger.info("searchTextEntry:{},queryCtx:{}", searchTextEntry, queryCtx);

        Set<SearchResult> searchResults = new LinkedHashSet();
        DomainInfoStat domainStat = NatureHelper.getDomainStat(originals);

        List<Integer> possibleDomains = getPossibleDomains(queryCtx, originals, domainStat);

        // 4.1 priority dimension metric
        boolean existMetricAndDimension = searchMetricAndDimension(new HashSet<>(possibleDomains), domainToName,
                searchTextEntry,
                searchResults);

        // 4.2 process based on dimension values
        MatchText matchText = searchTextEntry.getKey();
        Map<String, String> natureToNameMap = getNatureToNameMap(searchTextEntry);

        for (Map.Entry<String, String> natureToNameEntry : natureToNameMap.entrySet()) {
            searchDimensionValue(metricsDb, domainToName, domainStat.getMetricDomainCount(), searchResults,
                    existMetricAndDimension, matchText, natureToNameMap, natureToNameEntry);
        }
        return searchResults.stream().limit(RESULT_SIZE).collect(Collectors.toList());
    }

    private List<Integer> getPossibleDomains(QueryContextReq queryCtx, List<Term> originals,
            DomainInfoStat domainStat) {
        List<Integer> possibleDomains = NatureHelper.selectPossibleDomains(originals);

        Long contextDomain = chatService.getContextDomain(queryCtx.getChatId());

        logger.debug("possibleDomains:{},domainStat:{},contextDomain:{}", possibleDomains, domainStat, contextDomain);

        // If nothing is recognized or only metric are present, then add the contextDomain.
        if (nothingOrOnlyMetric(domainStat) && effectiveDomain(contextDomain)) {
            List<Integer> result = new ArrayList<>();
            result.add(Math.toIntExact(contextDomain));
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

    private void searchDimensionValue(List<ItemDO> metricsDb,
            Map<Integer, String> domainToName,
            long metricDomainCount,
            Set<SearchResult> searchResults,
            boolean existMetricAndDimension,
            MatchText matchText,
            Map<String, String> natureToNameMap,
            Map.Entry<String, String> natureToNameEntry) {
        String nature = natureToNameEntry.getKey();
        String wordName = natureToNameEntry.getValue();

        Integer domain = BaseWordNature.getDomain(nature);
        SchemaElementType schemaElementType = NatureConverter.convertTo(nature);

        if (SchemaElementType.ENTITY.equals(schemaElementType)) {
            return;
        }
        // If there are no metric/dimension, complete the  metric information
        if (metricDomainCount <= 0 && !existMetricAndDimension) {
            searchResults.add(
                    new SearchResult(matchText.getRegText() + wordName, wordName, domainToName.get(domain), domain,
                            schemaElementType));
            int metricSize = RESULT_SIZE / (natureToNameMap.entrySet().size());
            if (metricSize <= 1) {
                metricSize = 1;
            }
            List<String> metrics = filerMetricsByDomain(metricsDb, domain).stream().limit(metricSize).collect(
                    Collectors.toList());
            ;
            for (String metric : metrics) {
                String subRecommend = matchText.getRegText() + wordName + NatureType.SPACE + metric;
                searchResults.add(
                        new SearchResult(subRecommend, wordName + NatureType.SPACE + metric, domainToName.get(domain),
                                domain, false));
            }
        } else {
            searchResults.add(
                    new SearchResult(matchText.getRegText() + wordName, wordName, domainToName.get(domain), domain,
                            schemaElementType));
        }
    }

    protected List<String> filerMetricsByDomain(List<ItemDO> metricsDb, Integer domain) {
        if (CollectionUtils.isEmpty(metricsDb)) {
            return Lists.newArrayList();
        }
        return metricsDb.stream()
                .filter(mapDO -> Objects.nonNull(mapDO) && domain.equals(mapDO.getDomain()))
                .sorted(Comparator.comparing(ItemDO::getUseCnt).reversed())
                .flatMap(entry -> {
                    List<String> result = new ArrayList<>();
                    result.add(entry.getName());
                    return result.stream();
                })
                .collect(Collectors.toList());
    }

    /***
     * convert nature to name
     * @param recommendTextListEntry
     * @return
     */
    private Map<String, String> getNatureToNameMap(Map.Entry<MatchText, List<MapResult>> recommendTextListEntry) {
        List<MapResult> recommendValues = recommendTextListEntry.getValue();
        return recommendValues.stream()
                .flatMap(entry -> entry.getNatures().stream().map(nature -> {
                            WordNature posDO = new WordNature();
                            posDO.setWord(entry.getName());
                            posDO.setNature(nature);
                            return posDO;
                        }
                )).sorted(Comparator.comparingInt(a -> a.getWord().length()))
                .collect(Collectors.toMap(WordNature::getNature, WordNature::getWord, (value1, value2) -> value1,
                        LinkedHashMap::new));
    }

    private boolean searchMetricAndDimension(Set<Integer> possibleDomains, Map<Integer, String> domainToName,
            Map.Entry<MatchText, List<MapResult>> searchTextEntry, Set<SearchResult> searchResults) {
        boolean existMetric = false;

        MatchText matchText = searchTextEntry.getKey();
        List<MapResult> mapResults = searchTextEntry.getValue();

        for (MapResult mapResult : mapResults) {

            List<DomainWithSemanticType> dimensionMetricClassIds = mapResult.getNatures().stream()
                    .map(nature -> new DomainWithSemanticType(BaseWordNature.getDomain(nature),
                            NatureConverter.convertTo(nature)))
                    .filter(entry -> matchCondition(entry, possibleDomains)).collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(dimensionMetricClassIds)) {
                for (DomainWithSemanticType domainWithSemanticType : dimensionMetricClassIds) {
                    existMetric = true;
                    Integer domain = domainWithSemanticType.getDomain();
                    SchemaElementType semanticType = domainWithSemanticType.getSemanticType();
                    searchResults.add(
                            new SearchResult(matchText.getRegText() + mapResult.getName(), mapResult.getName(),
                                    domainToName.get(domain), domain, semanticType));
                }
            }
            logger.info("parseResult:{},dimensionMetricClassIds:{},possibleDomains:{}", mapResult,
                    dimensionMetricClassIds, possibleDomains);
        }
        return existMetric;
    }

    private boolean matchCondition(DomainWithSemanticType entry, Set<Integer> possibleDomains) {
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
