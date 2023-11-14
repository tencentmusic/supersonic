package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.knowledge.dictionary.FuzzyResult;
import com.tencent.supersonic.knowledge.utils.HanlpHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/***
 * A mapper capable of fuzzy parsing of metric names and dimension names.
 */
@Slf4j
public class FuzzyNameMapper extends BaseMapper {

    @Override
    public void doMap(QueryContext queryContext) {

        List<Term> terms = HanlpHelper.getTerms(queryContext.getRequest().getQueryText());

        FuzzyNameMatchStrategy fuzzyNameMatchStrategy = ContextUtils.getBean(FuzzyNameMatchStrategy.class);

        MapperHelper mapperHelper = ContextUtils.getBean(MapperHelper.class);

        List<FuzzyResult> matches = fuzzyNameMatchStrategy.getMatches(queryContext, terms);

        for (FuzzyResult match : matches) {
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
