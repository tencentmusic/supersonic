package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.*;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.DatabaseMapResult;
import com.tencent.supersonic.headless.chat.knowledge.DictWord;
import com.tencent.supersonic.headless.chat.knowledge.HanlpMapResult;
import com.tencent.supersonic.headless.chat.knowledge.KnowledgeBaseService;
import com.tencent.supersonic.headless.chat.knowledge.builder.BaseWordBuilder;
import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import com.tencent.supersonic.headless.chat.knowledge.helper.NatureHelper;
import com.tencent.supersonic.headless.chat.utils.DimensionValuesMatchUtils;
import com.tencent.supersonic.headless.chat.utils.EditDistanceUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * * A mapper that recognizes schema elements with keyword. It leverages two matching strategies:
 * HanlpDictMatchStrategy and DatabaseMatchStrategy.
 */
@Slf4j
public class KeywordMapper extends BaseMapper {

    @Override
    public void doMap(ChatQueryContext chatQueryContext) {
        log.info("Keyword Mapper start");
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
        Map<String, Object> transitionVauleAlias = dimValues(mapResults, chatQueryContext, terms);
        mapResults = (List<HanlpMapResult>) transitionVauleAlias.get("hanlpMapResult");
        terms = (List<S2Term>) transitionVauleAlias.get("term");
        Map<String, Long> wordNatureToFrequency =
                terms.stream().collect(Collectors.toMap(term -> term.getWord() + term.getNature(),
                        term -> Long.valueOf(term.getFrequency()), (value1, value2) -> value2));
        // 新增一个 Map 用于存储 SchemaElementType 和 nature 的映射关系
        Map<SchemaElementType, List<String>> elementTypeToNatureMap = new HashMap<>();
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
                // 存储 elementType 和 nature 到 map 中
                if (SchemaElementType.VALUE.equals(elementType)) {
                    elementTypeToNatureMap.computeIfAbsent(elementType, k -> new ArrayList<>())
                            .add(hanlpMapResult.getName() + nature);
                }
                Long elementID = NatureHelper.getElementID(nature);
                if (elementID == null) {
                    continue;
                }
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
        DimensionValuesMatchUtils dimensionValuesMatchUtils =
                ContextUtils.getBean(DimensionValuesMatchUtils.class);
        dimensionValuesMatchUtils.processDimensions(elementTypeToNatureMap, chatQueryContext);
    }

    private Map<String, Object> dimValues(List<HanlpMapResult> mapResults,
            ChatQueryContext chatQueryContext, List<S2Term> terms) {
        List<HanlpMapResult> hanlpMapList = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        for (HanlpMapResult hanlpMapResult : mapResults) {
            for (String nature : hanlpMapResult.getNatures()) {
                SchemaElementType elementType = NatureHelper.convertToElementType(nature);
                if (SchemaElementType.VALUE.equals(elementType)) {
                    Long elementID = NatureHelper.getElementID(nature);
                    SchemaElement elementDb =
                            chatQueryContext.getSemanticSchema().getElement(elementType, elementID);
                    if (Objects.isNull(elementDb)) {
                        hanlpMapList.add(hanlpMapResult);
                        continue;
                    }
                    List<DimValueMap> valueMapList = elementDb.getDimValueMaps();
                    if (CollectionUtils.isEmpty(valueMapList)) {
                        hanlpMapList.add(hanlpMapResult);
                        continue;
                    }
                    boolean find = false;
                    for (DimValueMap dimValueMap : valueMapList) {
                        for (String alias : dimValueMap.getAlias()) {
                            if (alias.equals(hanlpMapResult.getDetectWord())) {
                                HanlpMapResult hanlpMapResultN = new HanlpMapResult(
                                        hanlpMapResult.getName(), hanlpMapResult.getNatures(),
                                        hanlpMapResult.getDetectWord(),
                                        hanlpMapResult.getSimilarity());
                                BeanUtils.copyProperties(hanlpMapResult, hanlpMapResultN);
                                hanlpMapResultN.setDetectWord(dimValueMap.getValue());
                                hanlpMapResultN.setName(dimValueMap.getValue());
                                hanlpMapList.add(hanlpMapResultN);
                                find = true;
                                break;
                            }
                        }
                    }
                    if (!find) {
                        hanlpMapList.add(hanlpMapResult);
                    }
                } else {
                    hanlpMapList.add(hanlpMapResult);
                }
            }
        }
        List<S2Term> termList = new ArrayList<>();
        for (S2Term term : terms) {
            String nature = term.getNature().toString();
            SchemaElementType elementType = NatureHelper.convertToElementType(nature);
            if (SchemaElementType.VALUE.equals(elementType)) {
                Long elementID = NatureHelper.getElementID(nature);
                SchemaElement elementDb =
                        chatQueryContext.getSemanticSchema().getElement(elementType, elementID);
                if (Objects.isNull(elementDb)) {
                    termList.add(term);
                    continue;
                }
                List<DimValueMap> valueMapList = elementDb.getDimValueMaps();
                if (CollectionUtils.isEmpty(valueMapList)) {
                    termList.add(term);
                    continue;
                }
                boolean find = false;
                for (DimValueMap dimValueMap : valueMapList) {
                    for (String alias : dimValueMap.getAlias()) {
                        if (alias.equals(term.getWord())) {
                            S2Term s2TermN = new S2Term();
                            BeanUtils.copyProperties(term, s2TermN);
                            s2TermN.setWord(dimValueMap.getValue());
                            termList.add(s2TermN);
                            find = true;
                            break;
                        }
                    }
                }
                if (!find) {
                    termList.add(term);
                }
            } else {
                termList.add(term);
            }
        }
        map.put("hanlpMapResult", hanlpMapList);
        map.put("term", termList);
        return map;
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
