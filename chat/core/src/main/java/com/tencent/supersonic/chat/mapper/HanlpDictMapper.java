package com.tencent.supersonic.chat.mapper;

import com.hankcs.hanlp.seg.common.Term;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.knowledge.dictionary.HanlpMapResult;
import com.tencent.supersonic.knowledge.utils.HanlpHelper;
import com.tencent.supersonic.knowledge.utils.NatureHelper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    public void doMap(QueryContext queryContext) {

        String queryText = queryContext.getRequest().getQueryText();
        List<Term> terms = HanlpHelper.getTerms(queryText);

        HanlpDictMatchStrategy matchStrategy = ContextUtils.getBean(HanlpDictMatchStrategy.class);

        List<HanlpMapResult> matches = matchStrategy.getMatches(queryContext, terms);

        HanlpHelper.transLetterOriginal(matches);

        convertTermsToSchemaMapInfo(matches, queryContext.getMapInfo(), terms);
    }


    private void convertTermsToSchemaMapInfo(List<HanlpMapResult> hanlpMapResults, SchemaMapInfo schemaMap,
            List<Term> terms) {
        if (CollectionUtils.isEmpty(hanlpMapResults)) {
            return;
        }

        Map<String, Long> wordNatureToFrequency = terms.stream().collect(
                Collectors.toMap(entry -> entry.getWord() + entry.getNature(),
                        term -> Long.valueOf(term.getFrequency()), (value1, value2) -> value2));

        for (HanlpMapResult hanlpMapResult : hanlpMapResults) {
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
                SchemaElement element = getSchemaElement(modelId, elementType, elementID);
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

                addToSchemaMap(schemaMap, modelId, schemaElementMatch);
            }
        }
    }


}
