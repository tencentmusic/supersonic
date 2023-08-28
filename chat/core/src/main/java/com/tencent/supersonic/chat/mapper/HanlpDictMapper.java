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
import com.tencent.supersonic.chat.utils.NatureHelper;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.knowledge.dictionary.DictWordType;
import com.tencent.supersonic.knowledge.dictionary.MapResult;
import com.tencent.supersonic.knowledge.dictionary.builder.BaseWordBuilder;
import com.tencent.supersonic.knowledge.dictionary.builder.WordBuilderFactory;
import com.tencent.supersonic.knowledge.utils.HanlpHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
public class HanlpDictMapper implements SchemaMapper {

    @Override
    public void map(QueryContext queryContext) {

        String queryText = queryContext.getRequest().getQueryText();
        List<Term> terms = HanlpHelper.getTerms(queryText);

        for (Term term : terms) {
            log.info("word:{},nature:{},frequency:{}", term.word, term.nature.toString(), term.getFrequency());
        }
        Long modelId = queryContext.getRequest().getModelId();

        QueryMatchStrategy matchStrategy = ContextUtils.getBean(QueryMatchStrategy.class);
        Map<MatchText, List<MapResult>> matchResult = matchStrategy.match(queryText, terms, modelId);

        List<MapResult> matches = getMatches(matchResult);

        HanlpHelper.transLetterOriginal(matches);

        log.info("queryContext:{},matches:{}", queryContext, matches);

        convertTermsToSchemaMapInfo(matches, queryContext.getMapInfo(), terms);
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

                BaseWordBuilder baseWordBuilder = WordBuilderFactory.get(DictWordType.getNatureType(nature));
                Long elementID = baseWordBuilder.getElementID(nature);
                Long frequency = wordNatureToFrequency.get(mapResult.getName() + nature);

                SchemaElement element = modelSchema.getElement(elementType, elementID);
                if (Objects.isNull(element)) {
                    log.info("element is null, elementType:{},elementID:{}", elementType, elementID);
                    continue;
                }
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
}
