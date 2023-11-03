package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
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

/***
 * A mapper capable of prefix and suffix similarity parsing for
 * domain names, dimension values, metric names, and dimension names.
 */
@Slf4j
public class HanlpDictMapper extends BaseMapper {

    @Override
    public void work(QueryContext queryContext) {

        String queryText = queryContext.getRequest().getQueryText();
        List<Term> terms = HanlpHelper.getTerms(queryText);

        QueryMatchStrategy matchStrategy = ContextUtils.getBean(QueryMatchStrategy.class);

        Set<Long> detectModelIds = ContextUtils.getBean(MapperHelper.class).getModelIds(queryContext.getRequest());

        terms = filterByModelIds(terms, detectModelIds);

        Map<MatchText, List<MapResult>> matchResult = matchStrategy.match(queryContext.getRequest(), terms,
                detectModelIds);

        List<MapResult> matches = getMatches(matchResult);

        HanlpHelper.transLetterOriginal(matches);

        convertTermsToSchemaMapInfo(matches, queryContext.getMapInfo(), terms);
    }

    private List<Term> filterByModelIds(List<Term> terms, Set<Long> detectModelIds) {
        logTerms(terms);
        if (CollectionUtils.isNotEmpty(detectModelIds)) {
            terms = terms.stream().filter(term -> {
                Long modelId = NatureHelper.getModelId(term.getNature().toString());
                if (Objects.nonNull(modelId)) {
                    return detectModelIds.contains(modelId);
                }
                return false;
            }).collect(Collectors.toList());
            log.info("terms filter by modelIds:{}", detectModelIds);
            logTerms(terms);
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
                Long elementID = NatureHelper.getElementID(nature);
                SchemaElement element = getSchemaElement(modelId, elementType, elementID);
                if (element == null) {
                    continue;
                }
                if (element.getType().equals(SchemaElementType.VALUE)) {
                    element.setName(mapResult.getName());
                }
                Long frequency = wordNatureToFrequency.get(mapResult.getName() + nature);
                SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                        .element(element)
                        .frequency(frequency)
                        .word(mapResult.getName())
                        .similarity(mapResult.getSimilarity())
                        .detectWord(mapResult.getDetectWord())
                        .build();

                addToSchemaMap(schemaMap, modelId, schemaElementMatch);
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

}
