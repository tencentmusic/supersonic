package com.tencent.supersonic.chat.application.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.component.SchemaMapper;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.application.knowledge.NatureHelper;
import com.tencent.supersonic.chat.domain.pojo.search.MatchText;
import com.tencent.supersonic.chat.domain.utils.NatureConverter;
import com.tencent.supersonic.common.nlp.MapResult;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.knowledge.application.online.BaseWordNature;
import com.tencent.supersonic.knowledge.application.online.WordNatureStrategyFactory;
import com.tencent.supersonic.knowledge.infrastructure.nlp.HanlpHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
public class HanlpSchemaMapper implements SchemaMapper {

    @Override
    public void map(QueryContextReq queryContext) {

        List<Term> terms = HanlpHelper.getSegment().seg(queryContext.getQueryText().toLowerCase()).stream()
                .collect(Collectors.toList());

        terms.forEach(
                item -> log.info("word:{},nature:{},frequency:{}", item.word, item.nature.toString(),
                        item.getFrequency())
        );
        QueryMatchStrategy matchStrategy = ContextUtils.getBean(QueryMatchStrategy.class);

        Map<MatchText, List<MapResult>> matchResult = matchStrategy.match(queryContext.getQueryText(), terms,
                queryContext.getDomainId());
        List<MapResult> matches = new ArrayList<>();
        if (Objects.nonNull(matchResult)) {
            Optional<List<MapResult>> first = matchResult.entrySet().stream()
                    .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
                    .map(entry -> entry.getValue()).findFirst();
            if (first.isPresent()) {
                matches = first.get();
            }
        }
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
                Integer domain = NatureHelper.getDomain(nature);
                if (Objects.isNull(domain)) {
                    continue;
                }
                SchemaElementType elementType = NatureConverter.convertTo(nature);
                if (Objects.isNull(elementType)) {
                    continue;
                }

                BaseWordNature baseWordNature = WordNatureStrategyFactory.get(NatureType.getNatureType(nature));
                Integer elementID = baseWordNature.getElementID(nature);
                Long frequency = wordNatureToFrequency.get(mapResult.getName() + nature);
                SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                        .elementType(elementType)
                        .elementID(elementID)
                        .frequency(frequency)
                        .word(mapResult.getName())
                        .similarity(mapResult.getSimilarity())
                        .detectWord(mapResult.getDetectWord())
                        .build();

                Map<Integer, List<SchemaElementMatch>> domainElementMatches = schemaMap.getDomainElementMatches();
                List<SchemaElementMatch> schemaElementMatches = domainElementMatches.putIfAbsent(domain,
                        new ArrayList<>());
                if (schemaElementMatches == null) {
                    schemaElementMatches = domainElementMatches.get(domain);
                }
                schemaElementMatches.add(schemaElementMatch);
            }
        }
    }

}
