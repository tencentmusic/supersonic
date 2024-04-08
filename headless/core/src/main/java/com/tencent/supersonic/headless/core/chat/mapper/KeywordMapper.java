package com.tencent.supersonic.headless.core.chat.mapper;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.core.chat.knowledge.builder.BaseWordBuilder;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import com.tencent.supersonic.headless.core.chat.knowledge.DatabaseMapResult;
import com.tencent.supersonic.headless.core.chat.knowledge.HanlpMapResult;
import com.tencent.supersonic.headless.core.chat.knowledge.helper.HanlpHelper;
import com.tencent.supersonic.headless.core.chat.knowledge.helper.NatureHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        List<S2Term> terms = HanlpHelper.getTerms(queryText, queryContext.getModelIdToDataSetIds());
        HanlpDictMatchStrategy hanlpMatchStrategy = ContextUtils.getBean(HanlpDictMatchStrategy.class);

        List<HanlpMapResult> hanlpMapResults = hanlpMatchStrategy.getMatches(queryContext, terms);
        convertHanlpMapResultToMapInfo(hanlpMapResults, queryContext, terms);

        //2.database Match
        DatabaseMatchStrategy databaseMatchStrategy = ContextUtils.getBean(DatabaseMatchStrategy.class);

        List<DatabaseMapResult> databaseResults = databaseMatchStrategy.getMatches(queryContext, terms);
        convertDatabaseMapResultToMapInfo(queryContext, databaseResults);
    }

    private void convertHanlpMapResultToMapInfo(List<HanlpMapResult> mapResults, QueryContext queryContext,
                                                List<S2Term> terms) {
        if (CollectionUtils.isEmpty(mapResults)) {
            return;
        }
        HanlpHelper.transLetterOriginal(mapResults);
        Map<String, Long> wordNatureToFrequency = terms.stream().collect(
                Collectors.toMap(entry -> entry.getWord() + entry.getNature(),
                        term -> Long.valueOf(term.getFrequency()), (value1, value2) -> value2));

        for (HanlpMapResult hanlpMapResult : mapResults) {
            for (String nature : hanlpMapResult.getNatures()) {
                Long dataSetId = NatureHelper.getDataSetId(nature);
                if (Objects.isNull(dataSetId)) {
                    continue;
                }
                SchemaElementType elementType = NatureHelper.convertToElementType(nature);
                if (Objects.isNull(elementType)) {
                    continue;
                }
                Long elementID = NatureHelper.getElementID(nature);
                SchemaElement element = getSchemaElement(dataSetId, elementType,
                        elementID, queryContext.getSemanticSchema());
                if (element == null) {
                    continue;
                }
                Long frequency = wordNatureToFrequency.get(hanlpMapResult.getName() + nature);
                SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                        .element(element)
                        .frequency(frequency)
                        .word(hanlpMapResult.getName())
                        .similarity(hanlpMapResult.getSimilarity())
                        .detectWord(hanlpMapResult.getDetectWord())
                        .build();

                addToSchemaMap(queryContext.getMapInfo(), dataSetId, schemaElementMatch);
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
                    .frequency(BaseWordBuilder.DEFAULT_FREQUENCY)
                    .similarity(mapperHelper.getSimilarity(match.getDetectWord(), schemaElement.getName()))
                    .build();
            log.info("add to schema, elementMatch {}", schemaElementMatch);
            addToSchemaMap(queryContext.getMapInfo(), schemaElement.getDataSet(), schemaElementMatch);
        }
    }

    private Set<Long> getRegElementSet(SchemaMapInfo schemaMap, SchemaElement schemaElement) {
        List<SchemaElementMatch> elements = schemaMap.getMatchedElements(schemaElement.getDataSet());
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
