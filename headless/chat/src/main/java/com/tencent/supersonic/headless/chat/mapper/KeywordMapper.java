package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.DatabaseMapResult;
import com.tencent.supersonic.headless.chat.knowledge.DictWord;
import com.tencent.supersonic.headless.chat.knowledge.HanlpMapResult;
import com.tencent.supersonic.headless.chat.knowledge.KnowledgeBaseService;
import com.tencent.supersonic.headless.chat.knowledge.builder.BaseWordBuilder;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import com.tencent.supersonic.headless.chat.knowledge.helper.NatureHelper;
import com.tencent.supersonic.headless.chat.utils.EditDistanceUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * * A mapper that recognizes schema elements with keyword. It leverages two matching strategies:
 * HanlpDictMatchStrategy and DatabaseMatchStrategy.
 */
@Slf4j
public class KeywordMapper extends BaseMapper {

    @Override
    public void doMap(ChatQueryContext chatQueryContext) {
        String queryText = chatQueryContext.getRequest().getQueryText();

        // 1. hanlpDict Match
        List<S2Term> terms =
                HanlpHelper.getTerms(queryText, chatQueryContext.getModelIdToDataSetIds());
        HanlpDictMatchStrategy hanlpMatchStrategy =
                ContextUtils.getBean(HanlpDictMatchStrategy.class);
        List<HanlpMapResult> hanlpMatchResults = getMatches(chatQueryContext, hanlpMatchStrategy);
        convertMapResultToMapInfo(hanlpMatchResults, chatQueryContext, terms);

        // 2. database Match
        DatabaseMatchStrategy databaseMatchStrategy =
                ContextUtils.getBean(DatabaseMatchStrategy.class);
        List<DatabaseMapResult> databaseMatchResults =
                getMatches(chatQueryContext, databaseMatchStrategy);
        convertMapResultToMapInfo(chatQueryContext, databaseMatchResults);
    }

    private void convertMapResultToMapInfo(List<HanlpMapResult> mapResults,
            ChatQueryContext chatQueryContext, List<S2Term> terms) {
        if (CollectionUtils.isEmpty(mapResults)) {
            return;
        }

        HanlpHelper.transLetterOriginal(mapResults);
        Map<String, Long> wordNatureToFrequency =
                terms.stream().collect(Collectors.toMap(term -> term.getWord() + term.getNature(),
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
                SchemaElement element = getSchemaElement(dataSetId, elementType, elementID,
                        chatQueryContext.getSemanticSchema());
                if (Objects.isNull(element)) {
                    continue;
                }

                Long frequency = wordNatureToFrequency.get(hanlpMapResult.getName() + nature);
                SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                        .element(element).frequency(frequency).word(hanlpMapResult.getName())
                        .similarity(hanlpMapResult.getSimilarity())
                        .detectWord(hanlpMapResult.getDetectWord()).build();
                // doDimValueAliasLogic 将维度值别名进行替换成真实维度值
                doDimValueAliasLogic(schemaElementMatch);
                addToSchemaMap(chatQueryContext.getMapInfo(), dataSetId, schemaElementMatch);
            }
        }
    }

    private void doDimValueAliasLogic(SchemaElementMatch schemaElementMatch) {
        SchemaElement element = schemaElementMatch.getElement();
        if (SchemaElementType.VALUE.equals(element.getType())) {
            Long dimId = element.getId();
            String word = schemaElementMatch.getWord();
            Map<Long, List<DictWord>> dimValueAlias = KnowledgeBaseService.getDimValueAlias();
            if (Objects.nonNull(dimId) && StringUtils.isNotEmpty(word)
                    && dimValueAlias.containsKey(dimId)) {
                Map<String, DictWord> aliasAndDictMap = dimValueAlias.get(dimId).stream()
                        .collect(Collectors.toMap(dictWord -> dictWord.getAlias(),
                                dictWord -> dictWord, (v1, v2) -> v2));
                if (aliasAndDictMap.containsKey(word)) {
                    String wordTech = aliasAndDictMap.get(word).getWord();
                    schemaElementMatch.setWord(wordTech);
                }
            }
        }
    }

    private void convertMapResultToMapInfo(ChatQueryContext chatQueryContext,
            List<DatabaseMapResult> mapResults) {
        for (DatabaseMapResult match : mapResults) {
            SchemaElement schemaElement = match.getSchemaElement();
            Set<Long> regElementSet =
                    getRegElementSet(chatQueryContext.getMapInfo(), schemaElement);
            if (regElementSet.contains(schemaElement.getId())) {
                continue;
            }
            SchemaElementMatch schemaElementMatch = SchemaElementMatch.builder()
                    .element(schemaElement).word(schemaElement.getName())
                    .detectWord(match.getDetectWord()).frequency(BaseWordBuilder.DEFAULT_FREQUENCY)
                    .similarity(EditDistanceUtils.getSimilarity(match.getDetectWord(),
                            schemaElement.getName()))
                    .build();
            log.debug("add to schema, elementMatch {}", schemaElementMatch);
            addToSchemaMap(chatQueryContext.getMapInfo(), schemaElement.getDataSetId(),
                    schemaElementMatch);
        }
    }

    private Set<Long> getRegElementSet(SchemaMapInfo schemaMap, SchemaElement schemaElement) {
        List<SchemaElementMatch> elements =
                schemaMap.getMatchedElements(schemaElement.getDataSetId());
        if (CollectionUtils.isEmpty(elements)) {
            return new HashSet<>();
        }
        return elements.stream().filter(
                elementMatch -> SchemaElementType.METRIC.equals(elementMatch.getElement().getType())
                        || SchemaElementType.DIMENSION.equals(elementMatch.getElement().getType()))
                .map(elementMatch -> elementMatch.getElement().getId()).collect(Collectors.toSet());
    }
}
