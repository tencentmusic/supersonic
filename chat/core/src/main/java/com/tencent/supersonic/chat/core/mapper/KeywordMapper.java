package com.tencent.supersonic.chat.core.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.core.knowledge.DatabaseMapResult;
import com.tencent.supersonic.chat.core.knowledge.HanlpMapResult;
import com.tencent.supersonic.chat.core.utils.HanlpHelper;
import com.tencent.supersonic.chat.core.utils.NatureHelper;
import com.tencent.supersonic.common.util.ContextUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/***
 * A mapper that recognizes schema elements with keyword.
 * It leverages two matching strategies: HanlpDictMatchStrategy and DatabaseMatchStrategy.
 */
@Slf4j
public class KeywordMapper extends BaseMapper {

    @Override
    public void doMap(QueryContext queryContext) {
        String queryText = queryContext.getQueryText();
        //1.hanlpDict Match
        List<Term> terms = HanlpHelper.getTerms(queryText);
        HanlpDictMatchStrategy hanlpMatchStrategy = ContextUtils.getBean(HanlpDictMatchStrategy.class);

        List<HanlpMapResult> hanlpMapResults = hanlpMatchStrategy.getMatches(queryContext, terms);
        convertHanlpMapResultToMapInfo(hanlpMapResults, queryContext, terms);

        //2.database Match
        DatabaseMatchStrategy databaseMatchStrategy = ContextUtils.getBean(DatabaseMatchStrategy.class);

        List<DatabaseMapResult> databaseResults = databaseMatchStrategy.getMatches(queryContext, terms);
        convertDatabaseMapResultToMapInfo(queryContext, databaseResults);
    }

    private void convertHanlpMapResultToMapInfo(List<HanlpMapResult> mapResults, QueryContext queryContext,
            List<Term> terms) {
        if (CollectionUtils.isEmpty(mapResults)) {
            return;
        }
        HanlpHelper.transLetterOriginal(mapResults);
        Map<String, Long> wordNatureToFrequency = terms.stream().collect(
                Collectors.toMap(entry -> entry.getWord() + entry.getNature(),
                        term -> Long.valueOf(term.getFrequency()), (value1, value2) -> value2));

        for (HanlpMapResult hanlpMapResult : mapResults) {
            for (String nature : hanlpMapResult.getNatures()) {
                Long modelId = NatureHelper.getModelId(nature);
                if (Objects.isNull(modelId)) {
                    continue;
                }
                SchemaElementType elementType = NatureHelper.convertToElementType(nature);
                if (Objects.isNull(elementType)) {
                    continue;
                }
                Long elementID = NatureHelper.getElementID(nature);
                SchemaElement element = getSchemaElement(modelId, elementType, elementID,
                        queryContext.getSemanticSchema());
                if (element == null) {
                    continue;
                }
                if (element.getType().equals(SchemaElementType.VALUE)) {
                    element.setName(hanlpMapResult.getName());
                }
                Long frequency = wordNatureToFrequency.get(hanlpMapResult.getName() + nature);
                SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                        .element(element)
                        .frequency(frequency)
                        .word(hanlpMapResult.getName())
                        .similarity(hanlpMapResult.getSimilarity())
                        .detectWord(hanlpMapResult.getDetectWord())
                        .build();

                addToSchemaMap(queryContext.getMapInfo(), modelId, schemaElementMatch);
            }
        }
    }

    private void convertDatabaseMapResultToMapInfo(QueryContext queryContext, List<DatabaseMapResult> mapResults) {
        MapperHelper mapperHelper = ContextUtils.getBean(MapperHelper.class);
        for (DatabaseMapResult match : mapResults) {
            SchemaElement schemaElement = match.getSchemaElement();
            Set<Long> regElementSet = getRegElementSet(queryContext.getMapInfo(), schemaElement);
            if (regElementSet.contains(schemaElement.getId())) {
                continue;
            }
            SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                    .element(schemaElement)
                    .word(schemaElement.getName())
                    .detectWord(match.getDetectWord())
                    .frequency(10000L)
                    .similarity(mapperHelper.getSimilarity(match.getDetectWord(), schemaElement.getName()))
                    .build();
            log.info("add to schema, elementMatch {}", schemaElementMatch);
            addToSchemaMap(queryContext.getMapInfo(), schemaElement.getModel(), schemaElementMatch);
        }
    }

    private Set<Long> getRegElementSet(SchemaMapInfo schemaMap, SchemaElement schemaElement) {
        List<SchemaElementMatch> elements = schemaMap.getMatchedElements(schemaElement.getModel());
        if (CollectionUtils.isEmpty(elements)) {
            return new HashSet<>();
        }
        return elements.stream()
                .filter(elementMatch ->
                        SchemaElementType.METRIC.equals(elementMatch.getElement().getType())
                                || SchemaElementType.DIMENSION.equals(elementMatch.getElement().getType()))
                .map(elementMatch -> elementMatch.getElement().getId())
                .collect(Collectors.toSet());
    }
}
