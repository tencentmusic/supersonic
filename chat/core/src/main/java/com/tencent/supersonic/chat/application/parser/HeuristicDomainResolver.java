package com.tencent.supersonic.chat.application.parser;

import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class HeuristicDomainResolver implements DomainResolver {

    protected static Integer selectDomainBySchemaElementCount(Map<Integer, SemanticQuery> domainQueryModes,
            SchemaMapInfo schemaMap) {
        Map<Integer, QueryMatchInfo> domainTypeMap = getDomainTypeMap(schemaMap);
        if (domainTypeMap.size() == 1) {
            Integer domainSelect = domainTypeMap.entrySet().stream().collect(Collectors.toList()).get(0).getKey();
            if (domainQueryModes.containsKey(domainSelect)) {
                log.info("selectDomain from domainTypeMap not order [{}]", domainSelect);
                return domainSelect;
            }
        } else {
            Map.Entry<Integer, QueryMatchInfo> maxDomain = domainTypeMap.entrySet().stream()
                    .filter(entry -> domainQueryModes.containsKey(entry.getKey()))
                    .sorted(ContextHelper.DomainStatComparator).findFirst().orElse(null);
            if (maxDomain != null) {
                log.info("selectDomain from domainTypeMap  order [{}]", maxDomain.getKey());
                return maxDomain.getKey();
            }
        }
        return 0;
    }

    /**
     * to check can switch domain if context exit domain
     *
     * @return false will use context domain, true will use other domain , maybe include context domain
     */
    protected static boolean isAllowSwitch(Map<Integer, SemanticQuery> domainQueryModes, SchemaMapInfo schemaMap,
            ChatContext chatCtx, QueryContextReq searchCtx, Integer domainId) {
        if (!Objects.nonNull(domainId) || domainId <= 0) {
            return true;
        }
        // except content domain, calculate the number of types for each domain, if numbers<=1 will not switch
        Map<Integer, QueryMatchInfo> domainTypeMap = getDomainTypeMap(schemaMap);
        log.info("isAllowSwitch domainTypeMap [{}]", domainTypeMap);
        long otherDomainTypeNumBigOneCount = domainTypeMap.entrySet().stream()
                .filter(entry -> domainQueryModes.containsKey(entry.getKey()) && !entry.getKey().equals(domainId))
                .filter(entry -> entry.getValue().getCount() > 1).count();
        if (otherDomainTypeNumBigOneCount >= 1) {
            return true;
        }
        // if query text only contain time , will not switch
        for (SemanticQuery semanticQuery : domainQueryModes.values()) {
            SemanticParseInfo semanticParseInfo = semanticQuery.getParseInfo();
            if (semanticParseInfo == null) {
                continue;
            }
            if (searchCtx.getQueryText() != null && semanticParseInfo.getDateInfo() != null) {
                if (semanticParseInfo.getDateInfo().getText() != null) {
                    if (semanticParseInfo.getDateInfo().getText().equalsIgnoreCase(searchCtx.getQueryText())) {
                        log.info("timeParseResults is not null , can not switch context , timeParseResults:{},",
                                semanticParseInfo.getDateInfo());
                        return false;
                    }
                }
            }
        }

        // if context domain not  in schemaMap , will switch
        if (schemaMap.getMatchedElements(domainId) == null || schemaMap.getMatchedElements(domainId).size() <= 0) {
            log.info("domainId not in schemaMap ");
            return true;
        }
        // other will not switch
        return false;
    }

    public static Map<Integer, QueryMatchInfo> getDomainTypeMap(SchemaMapInfo schemaMap) {
        Map<Integer, QueryMatchInfo> domainCount = new HashMap<>();
        for (Map.Entry<Integer, List<SchemaElementMatch>> entry : schemaMap.getDomainElementMatches().entrySet()) {
            List<SchemaElementMatch> schemaElementMatches = schemaMap.getMatchedElements(entry.getKey());
            if (schemaElementMatches != null && schemaElementMatches.size() > 0) {
                if (!domainCount.containsKey(entry.getKey())) {
                    domainCount.put(entry.getKey(), new QueryMatchInfo());
                }
                QueryMatchInfo queryMatchInfo = domainCount.get(entry.getKey());
                Set<SchemaElementType> schemaElementTypes = new HashSet<>();
                schemaElementMatches.stream()
                        .forEach(schemaElementMatch -> schemaElementTypes.add(schemaElementMatch.getElementType()));
                SchemaElementMatch schemaElementMatchMax = schemaElementMatches.stream()
                        .sorted(ContextHelper.schemaElementMatchComparatorBySimilarity).findFirst().orElse(null);
                if (schemaElementMatchMax != null) {
                    queryMatchInfo.setMaxSimilarity(schemaElementMatchMax.getSimilarity());
                }
                queryMatchInfo.setCount(schemaElementTypes.size());

            }
        }
        return domainCount;
    }

    @Override
    public boolean isDomainSwitch(ChatContext chatCtx, SemanticParseInfo semanticParseInfo) {
        Long contextDomain = chatCtx.getParseInfo().getDomainId();
        Long currentDomain = semanticParseInfo.getDomainId();
        boolean noSwitch =
                currentDomain == null || contextDomain == null || contextDomain.equals(currentDomain);
        log.debug("ChatContext isDomainSwitch [{}] [{}]",
                semanticParseInfo.getQueryMode(), !noSwitch);
        return !noSwitch;
    }

    @Override
    public Integer resolve(Map<Integer, SemanticQuery> domainQueryModes, QueryContextReq searchCtx,
            ChatContext chatCtx, SchemaMapInfo schemaMap) {
        Integer selectDomain = selectDomain(domainQueryModes, searchCtx, chatCtx, schemaMap);
        if (selectDomain > 0) {
            log.info("selectDomain {} ", selectDomain);
            return selectDomain;
        }
        // get the max SchemaElementType number
        return selectDomainBySchemaElementCount(domainQueryModes, schemaMap);
    }

    public Integer selectDomain(Map<Integer, SemanticQuery> domainQueryModes, QueryContextReq searchCtx,
            ChatContext chatCtx,
            SchemaMapInfo schemaMap) {
        // if QueryContext has domainId and in domainQueryModes
        if (domainQueryModes.containsKey(searchCtx.getDomainId())) {
            log.info("selectDomain from QueryContext [{}]", searchCtx.getDomainId());
            return searchCtx.getDomainId();
        }
        // if ChatContext has domainId and in domainQueryModes
        if (chatCtx.getParseInfo().getDomainId() > 0) {
            Integer domainId = Integer.valueOf(chatCtx.getParseInfo().getDomainId().intValue());
            if (!isAllowSwitch(domainQueryModes, schemaMap, chatCtx, searchCtx, domainId)) {
                log.info("selectDomain from ChatContext [{}]", domainId);
                return domainId;
            }
        }
        // default 0
        return 0;
    }
}