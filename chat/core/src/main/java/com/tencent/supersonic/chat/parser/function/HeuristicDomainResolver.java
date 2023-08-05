package com.tencent.supersonic.chat.parser.function;

import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
public class HeuristicDomainResolver implements DomainResolver {

    protected static Long selectDomainBySchemaElementCount(Map<Long, SemanticQuery> domainQueryModes,
            SchemaMapInfo schemaMap) {
        Map<Long, DomainMatchResult> domainTypeMap = getDomainTypeMap(schemaMap);
        if (domainTypeMap.size() == 1) {
            Long domainSelect = domainTypeMap.entrySet().stream().collect(Collectors.toList()).get(0).getKey();
            if (domainQueryModes.containsKey(domainSelect)) {
                log.info("selectDomain with only one domain [{}]", domainSelect);
                return domainSelect;
            }
        } else {
            Map.Entry<Long, DomainMatchResult> maxDomain = domainTypeMap.entrySet().stream()
                    .filter(entry -> domainQueryModes.containsKey(entry.getKey()))
                    .sorted((o1, o2) -> {
                        int difference = o2.getValue().getCount() - o1.getValue().getCount();
                        if (difference == 0) {
                            return (int) ((o2.getValue().getMaxSimilarity()
                                    - o1.getValue().getMaxSimilarity()) * 100);
                        }
                        return difference;
                    }).findFirst().orElse(null);
            if (maxDomain != null) {
                log.info("selectDomain with multiple domains [{}]", maxDomain.getKey());
                return maxDomain.getKey();
            }
        }
        return 0L;
    }

    /**
     * to check can switch domain if context exit domain
     *
     * @return false will use context domain, true will use other domain , maybe include context domain
     */
    protected static boolean isAllowSwitch(Map<Long, SemanticQuery> domainQueryModes, SchemaMapInfo schemaMap,
            ChatContext chatCtx, QueryReq searchCtx, Long domainId, List<Long> restrictiveDomains) {
        if (!Objects.nonNull(domainId) || domainId <= 0) {
            return true;
        }
        // except content domain, calculate the number of types for each domain, if numbers<=1 will not switch
        Map<Long, DomainMatchResult> domainTypeMap = getDomainTypeMap(schemaMap);
        log.info("isAllowSwitch domainTypeMap [{}]", domainTypeMap);
        long otherDomainTypeNumBigOneCount = domainTypeMap.entrySet().stream()
                .filter(entry -> domainQueryModes.containsKey(entry.getKey()) && !entry.getKey().equals(domainId))
                .filter(entry -> entry.getValue().getCount() > 1).count();
        if (otherDomainTypeNumBigOneCount >= 1) {
            return true;
        }
        // if query text only contain time , will not switch
        if (!CollectionUtils.isEmpty(domainQueryModes.values())) {
            for (SemanticQuery semanticQuery : domainQueryModes.values()) {
                if (semanticQuery == null) {
                    continue;
                }
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
        }

        if (CollectionUtils.isNotEmpty(restrictiveDomains) && !restrictiveDomains.contains(domainId)) {
            return true;
        }
        // if context domain not  in schemaMap , will switch
        if (schemaMap.getMatchedElements(domainId) == null || schemaMap.getMatchedElements(domainId).size() <= 0) {
            log.info("domainId not in schemaMap ");
            return true;
        }
        // other will not switch
        return false;
    }

    public static Map<Long, DomainMatchResult> getDomainTypeMap(SchemaMapInfo schemaMap) {
        Map<Long, DomainMatchResult> domainCount = new HashMap<>();
        for (Map.Entry<Long, List<SchemaElementMatch>> entry : schemaMap.getDomainElementMatches().entrySet()) {
            List<SchemaElementMatch> schemaElementMatches = schemaMap.getMatchedElements(entry.getKey());
            if (schemaElementMatches != null && schemaElementMatches.size() > 0) {
                if (!domainCount.containsKey(entry.getKey())) {
                    domainCount.put(entry.getKey(), new DomainMatchResult());
                }
                DomainMatchResult domainMatchResult = domainCount.get(entry.getKey());
                Set<SchemaElementType> schemaElementTypes = new HashSet<>();
                schemaElementMatches.stream()
                        .forEach(schemaElementMatch -> schemaElementTypes.add(
                                schemaElementMatch.getElement().getType()));
                SchemaElementMatch schemaElementMatchMax = schemaElementMatches.stream()
                        .sorted((o1, o2) ->
                                ((int) ((o2.getSimilarity() - o1.getSimilarity()) * 100))
                        ).findFirst().orElse(null);
                if (schemaElementMatchMax != null) {
                    domainMatchResult.setMaxSimilarity(schemaElementMatchMax.getSimilarity());
                }
                domainMatchResult.setCount(schemaElementTypes.size());

            }
        }
        return domainCount;
    }


    public Long resolve(QueryContext queryContext, ChatContext chatCtx, List<Long> restrictiveDomains) {
        Long domainId = queryContext.getRequest().getDomainId();
        if (Objects.nonNull(domainId) && domainId > 0) {
            if (CollectionUtils.isNotEmpty(restrictiveDomains) && restrictiveDomains.contains(domainId)) {
                return domainId;
            } else {
                return null;
            }
        }
        SchemaMapInfo mapInfo = queryContext.getMapInfo();
        Set<Long> matchedDomains = mapInfo.getMatchedDomains();
        if (CollectionUtils.isNotEmpty(restrictiveDomains)) {
            matchedDomains = matchedDomains.stream()
                    .filter(matchedDomain -> restrictiveDomains.contains(matchedDomain))
                    .collect(Collectors.toSet());
        }
        Map<Long, SemanticQuery> domainQueryModes = new HashMap<>();
        for (Long matchedDomain : matchedDomains) {
            domainQueryModes.put(matchedDomain, null);
        }
        if(domainQueryModes.size()==1){
            return domainQueryModes.keySet().stream().findFirst().get();
        }
        return resolve(domainQueryModes, queryContext, chatCtx,
                queryContext.getMapInfo(),restrictiveDomains);
    }

    public Long resolve(Map<Long, SemanticQuery> domainQueryModes, QueryContext queryContext,
            ChatContext chatCtx, SchemaMapInfo schemaMap, List<Long> restrictiveDomains) {
        Long selectDomain = selectDomain(domainQueryModes, queryContext.getRequest(), chatCtx, schemaMap,restrictiveDomains);
        if (selectDomain > 0) {
            log.info("selectDomain {} ", selectDomain);
            return selectDomain;
        }
        // get the max SchemaElementType number
        return selectDomainBySchemaElementCount(domainQueryModes, schemaMap);
    }

    public Long selectDomain(Map<Long, SemanticQuery> domainQueryModes, QueryReq queryContext,
            ChatContext chatCtx,
            SchemaMapInfo schemaMap, List<Long> restrictiveDomains) {
        // if QueryContext has domainId and in domainQueryModes
        if (domainQueryModes.containsKey(queryContext.getDomainId())) {
            log.info("selectDomain from QueryContext [{}]", queryContext.getDomainId());
            return queryContext.getDomainId();
        }
        // if ChatContext has domainId and in domainQueryModes
        if (chatCtx.getParseInfo().getDomainId() > 0) {
            Long domainId = chatCtx.getParseInfo().getDomainId();
            if (!isAllowSwitch(domainQueryModes, schemaMap, chatCtx, queryContext, domainId,restrictiveDomains)) {
                log.info("selectDomain from ChatContext [{}]", domainId);
                return domainId;
            }
        }
        // default 0
        return 0L;
    }
}
