package com.tencent.supersonic.chat.application.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.application.knowledge.WordNatureService;
import com.tencent.supersonic.chat.domain.pojo.chat.DomainInfos;
import com.tencent.supersonic.common.nlp.ItemDO;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.knowledge.infrastructure.nlp.HanlpHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class DatabaseSchemaMapper implements SchemaMapper {

    @Override
    public void map(QueryContextReq queryContext) {

        log.debug("before db mapper,mapInfo:{}", queryContext.getMapInfo());

        List<Term> terms = HanlpHelper.getSegment().seg(queryContext.getQueryText().toLowerCase()).stream()
                .collect(Collectors.toList());

        WordNatureService wordNatureService = ContextUtils.getBean(WordNatureService.class);

        DomainInfos domainInfos = wordNatureService.getCache().getUnchecked("");

        detectAndAddToSchema(queryContext, terms, domainInfos.getDimensions(),
                SchemaElementType.DIMENSION);
        detectAndAddToSchema(queryContext, terms, domainInfos.getMetrics(), SchemaElementType.METRIC);

        log.debug("after db mapper,mapInfo:{}", queryContext.getMapInfo());
    }

    private void detectAndAddToSchema(QueryContextReq queryContext, List<Term> terms, List<ItemDO> domains,
            SchemaElementType schemaElementType) {
        try {
            String queryText = queryContext.getQueryText();

            Map<String, Set<ItemDO>> domainResultSet = getResultSet(queryText, terms, domains);

            addToSchemaMapInfo(domainResultSet, queryContext.getMapInfo(), schemaElementType);

        } catch (Exception e) {
            log.error("detectAndAddToSchema error", e);
        }
    }

    private Map<String, Set<ItemDO>> getResultSet(String queryText, List<Term> terms, List<ItemDO> domains) {

        MapperHelper mapperHelper = ContextUtils.getBean(MapperHelper.class);

        Map<String, Set<ItemDO>> nameToItems = getNameToItems(domains);

        Map<Integer, Integer> regOffsetToLength = terms.stream().sorted(Comparator.comparing(Term::length))
                .collect(Collectors.toMap(Term::getOffset, term -> term.word.length(), (value1, value2) -> value2));

        Map<String, Set<ItemDO>> domainResultSet = new HashMap<>();
        for (Integer index = 0; index <= queryText.length() - 1; ) {
            for (Integer i = index; i <= queryText.length(); ) {
                i = mapperHelper.getStepIndex(regOffsetToLength, i);
                if (i <= queryText.length()) {
                    String detectSegment = queryText.substring(index, i);
                    nameToItems.forEach(
                            (name, newItemDOs) -> {
                                if (name.contains(detectSegment)
                                        && mapperHelper.getSimilarity(detectSegment, name)
                                        >= mapperHelper.getMetricDimensionThresholdConfig()) {
                                    Set<ItemDO> preItemDOS = domainResultSet.putIfAbsent(detectSegment, newItemDOs);
                                    if (Objects.nonNull(preItemDOS)) {
                                        preItemDOS.addAll(newItemDOs);
                                    }
                                }
                            }
                    );
                }
            }
            index = mapperHelper.getStepIndex(regOffsetToLength, index);
        }
        return domainResultSet;
    }

    private Map<String, Set<ItemDO>> getNameToItems(List<ItemDO> domains) {
        return domains.stream()
                .collect(Collectors.toMap(ItemDO::getName, a -> {
                    Set<ItemDO> result = new HashSet<>();
                    result.add(a);
                    return result;
                }, (k1, k2) -> {
                    k1.addAll(k2);
                    return k1;
                }));
    }

    private void addToSchemaMapInfo(Map<String, Set<ItemDO>> mapResultRowSet, SchemaMapInfo schemaMap,
            SchemaElementType schemaElementType) {
        if (Objects.isNull(mapResultRowSet) || mapResultRowSet.size() <= 0) {
            return;
        }
        MapperHelper mapperHelper = ContextUtils.getBean(MapperHelper.class);

        for (Map.Entry<String, Set<ItemDO>> entry : mapResultRowSet.entrySet()) {
            String detectWord = entry.getKey();
            Set<ItemDO> itemDOS = entry.getValue();
            for (ItemDO itemDO : itemDOS) {

                List<SchemaElementMatch> elements = schemaMap.getMatchedElements(itemDO.getDomain());
                if (CollectionUtils.isEmpty(elements)) {
                    elements = new ArrayList<>();
                    schemaMap.setMatchedElements(itemDO.getDomain(), elements);
                }
                Set<Integer> regElementSet = elements.stream()
                        .filter(elementMatch -> schemaElementType.equals(elementMatch.getElementType()))
                        .map(elementMatch -> elementMatch.getElementID())
                        .collect(Collectors.toSet());

                if (regElementSet.contains(itemDO.getItemId())) {
                    continue;
                }
                SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                        .elementID(itemDO.getItemId()).word(itemDO.getName()).frequency(10000L)
                        .elementType(schemaElementType).detectWord(detectWord)
                        .similarity(mapperHelper.getSimilarity(detectWord, itemDO.getName()))
                        .build();
                log.info("schemaElementType:{},add to schema, elementMatch {}", schemaElementType, schemaElementMatch);
                elements.add(schemaElementMatch);
            }
        }

    }
}
