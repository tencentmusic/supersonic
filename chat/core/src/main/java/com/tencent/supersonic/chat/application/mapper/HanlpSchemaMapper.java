package com.tencent.supersonic.chat.application.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SchemaMapper;
import com.tencent.supersonic.chat.application.knowledge.NatureHelper;
import com.tencent.supersonic.chat.domain.utils.NatureConverter;
import com.tencent.supersonic.common.nlp.MapResult;
import com.tencent.supersonic.common.nlp.NatureType;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.common.util.json.JsonUtil;
import com.tencent.supersonic.knowledge.application.online.BaseWordNature;
import com.tencent.supersonic.knowledge.application.online.WordNatureStrategyFactory;
import com.tencent.supersonic.knowledge.infrastructure.nlp.HanlpHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HanlpSchemaMapper implements SchemaMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HanlpSchemaMapper.class);

    @Override
    public void map(QueryContextReq queryContext) {

        List<Term> terms = HanlpHelper.getSegment().seg(queryContext.getQueryText().toLowerCase()).stream()
                .collect(Collectors.toList());

        terms.forEach(
                item -> LOGGER.info("word:{},nature:{},frequency:{}", item.word, item.nature.toString(),
                        item.getFrequency())
        );
        QueryMatchStrategy matchStrategy = ContextUtils.getBean(QueryMatchStrategy.class);

        List<MapResult> matches = matchStrategy.match(queryContext.getQueryText(), terms, queryContext.getDomainId());
        HanlpHelper.transLetterOriginal(matches);
        LOGGER.info("queryContext:{},matches:{}", queryContext, matches);

        convertTermsToSchemaMapInfo(matches, queryContext.getMapInfo());
    }

    private void convertTermsToSchemaMapInfo(List<MapResult> mapResults, SchemaMapInfo schemaMap) {
        if (CollectionUtils.isEmpty(mapResults)) {
            return;
        }
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
                SchemaElementMatch schemaElementMatch = new SchemaElementMatch();

                schemaElementMatch.setElementType(elementType);
                BaseWordNature baseWordNature = WordNatureStrategyFactory.get(NatureType.getNatureType(nature));
                Integer elementID = baseWordNature.getElementID(nature);
                schemaElementMatch.setElementID(elementID);
                Long frequency = baseWordNature.getFrequency(nature);
                schemaElementMatch.setFrequency(frequency);
                schemaElementMatch.setWord(mapResult.getName());
                schemaElementMatch.setSimilarity(mapResult.getSimilarity());
                schemaElementMatch.setDetectWord(mapResult.getDetectWord());

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
