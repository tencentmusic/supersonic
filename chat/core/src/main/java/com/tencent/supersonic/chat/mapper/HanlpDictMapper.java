package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.knowledge.dictionary.MapResult;
import com.tencent.supersonic.knowledge.utils.HanlpHelper;
import com.tencent.supersonic.knowledge.utils.NatureHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

@Slf4j
public class HanlpDictMapper implements SchemaMapper {

    @Override
    public void map(QueryContext queryContext) {

        String queryText = queryContext.getRequest().getQueryText();
        List<Term> terms = HanlpHelper.getTerms(queryText);

        QueryMatchStrategy matchStrategy = ContextUtils.getBean(QueryMatchStrategy.class);
        MapperHelper mapperHelper = ContextUtils.getBean(MapperHelper.class);
        Set<Long> detectModelIds = mapperHelper.getModelIds(queryContext.getRequest());

        terms = filterByModelIds(terms, detectModelIds);

        Map<MatchText, List<MapResult>> matchResult = matchStrategy.match(queryContext.getRequest(), terms,
                detectModelIds);

        List<MapResult> matches = getMatches(matchResult);

        HanlpHelper.transLetterOriginal(matches);

        log.info("queryContext:{},matches:{}", queryContext, matches);

        convertTermsToSchemaMapInfo(matches, queryContext.getMapInfo(), terms);
    }

    private List<Term> filterByModelIds(List<Term> terms, Set<Long> detectModelIds) {
        for (Term term : terms) {
            log.info("before word:{},nature:{},frequency:{}", term.word, term.nature.toString(), term.getFrequency());
        }
        if (CollectionUtils.isNotEmpty(detectModelIds)) {
            terms = terms.stream().filter(term -> {
                Long modelId = NatureHelper.getModelId(term.getNature().toString());
                if (Objects.nonNull(modelId)) {
                    return detectModelIds.contains(modelId);
                }
                return false;
            }).collect(Collectors.toList());
        }
        for (Term term : terms) {
            log.info("after filter word:{},nature:{},frequency:{}", term.word, term.nature.toString(),
                    term.getFrequency());
        }
        return terms;
    }


    private void convertTermsToSchemaMapInfo(List<MapResult> mapResults, SchemaMapInfo schemaMap, List<Term> terms) {
        if (CollectionUtils.isEmpty(mapResults)) {
            return;
        }

        Map<String, Long> wordNatureToFrequency = terms.stream().collect(
                Collectors.toMap(entry -> entry.getWord() + entry.getNature(),
                        term -> Long.valueOf(term.getFrequency()), (value1, value2) -> value2));

        for (MapResult mapResult : mapResults) {
            for (String nature : mapResult.getNatures()) {
                Long modelId = NatureHelper.getModelId(nature);
                if (Objects.isNull(modelId)) {
                    continue;
                }
                SchemaElementType elementType = NatureHelper.convertToElementType(nature);
                if (Objects.isNull(elementType)) {
                    continue;
                }

                SemanticService schemaService = ContextUtils.getBean(SemanticService.class);
                ModelSchema modelSchema = schemaService.getModelSchema(modelId);
                if (Objects.isNull(modelSchema)) {
                    return;
                }

                Long elementID = NatureHelper.getElementID(nature);
                Long frequency = wordNatureToFrequency.get(mapResult.getName() + nature);

                SchemaElement elementDb = modelSchema.getElement(elementType, elementID);
                if (Objects.isNull(elementDb)) {
                    log.info("element is null, elementType:{},elementID:{}", elementType, elementID);
                    continue;
                }
                SchemaElement element = new SchemaElement();
                BeanUtils.copyProperties(elementDb, element);
                element.setAlias(getAlias(elementDb));
                if (element.getType().equals(SchemaElementType.VALUE)) {
                    element.setName(mapResult.getName());
                }
                SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                        .element(element)
                        .frequency(frequency)
                        .word(mapResult.getName())
                        .similarity(mapResult.getSimilarity())
                        .detectWord(mapResult.getDetectWord())
                        .build();

                Map<Long, List<SchemaElementMatch>> modelElementMatches = schemaMap.getModelElementMatches();
                List<SchemaElementMatch> schemaElementMatches = modelElementMatches.putIfAbsent(modelId,
                        new ArrayList<>());
                if (schemaElementMatches == null) {
                    schemaElementMatches = modelElementMatches.get(modelId);
                }
                schemaElementMatches.add(schemaElementMatch);
            }
        }
    }

    private List<MapResult> getMatches(Map<MatchText, List<MapResult>> matchResult) {
        List<MapResult> matches = new ArrayList<>();
        if (Objects.isNull(matchResult)) {
            return matches;
        }
        Optional<List<MapResult>> first = matchResult.entrySet().stream()
                .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
                .map(entry -> entry.getValue()).findFirst();

        if (first.isPresent()) {
            matches = first.get();
        }
        return matches;
    }

    public List<String> getAlias(SchemaElement element) {
        if (!SchemaElementType.VALUE.equals(element.getType())) {
            return element.getAlias();
        }
        if (CollectionUtils.isNotEmpty(element.getAlias()) && StringUtils.isNotEmpty(element.getName())) {
            return element.getAlias().stream()
                    .filter(aliasItem -> aliasItem.contains(element.getName()))
                    .collect(Collectors.toList());
        }
        return element.getAlias();
    }
}
