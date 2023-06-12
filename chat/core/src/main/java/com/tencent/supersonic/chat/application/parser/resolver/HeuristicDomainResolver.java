package com.tencent.supersonic.chat.application.parser.resolver;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementCount;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SemanticQuery;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Service("heuristicDomainResolver")
@Primary
public class HeuristicDomainResolver implements DomainResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeuristicDomainResolver.class);

    @Override
    public Integer resolve(Map<Integer, SemanticQuery> domainQueryModes, QueryContextReq searchCtx,
            ChatContext chatCtx, SchemaMapInfo schemaMap) {
        // if QueryContext has domainId and in domainQueryModes
        if (domainQueryModes.containsKey(searchCtx.getDomainId())) {
            LOGGER.info("selectDomain from QueryContext [{}]", searchCtx.getDomainId());
            return searchCtx.getDomainId();
        }
        // if ChatContext has domainId and in domainQueryModes
        if (chatCtx.getParseInfo().getDomainId() > 0) {
            Integer domainId = Integer.valueOf(chatCtx.getParseInfo().getDomainId().intValue());
            if (!isAllowSwitch(domainQueryModes, schemaMap, chatCtx, searchCtx, domainId)) {
                LOGGER.info("selectDomain from ChatContext [{}]", domainId);
                return domainId;
            }
        }
        // get the max SchemaElementType number
        Map<Integer, SchemaElementCount> domainTypeMap = getDomainTypeMap(schemaMap);
        if (domainTypeMap.size() == 1) {
            Integer domainSelect = domainTypeMap.entrySet().stream().collect(Collectors.toList()).get(0).getKey();
            if (domainQueryModes.containsKey(domainSelect)) {
                LOGGER.info("selectDomain from domainTypeMap not order [{}]", domainSelect);
                return domainSelect;
            }
        } else {
            Map.Entry<Integer, SchemaElementCount> maxDomain = domainTypeMap.entrySet().stream()
                    .filter(entry -> domainQueryModes.containsKey(entry.getKey()))
                    .sorted(ContextHelper.DomainStatComparator).findFirst().orElse(null);
            if (maxDomain != null) {
                LOGGER.info("selectDomain from domainTypeMap need order [{}]", maxDomain.getKey());
                return maxDomain.getKey();
            }
        }
        // bad case , here will not reach , default 0
        LOGGER.error("selectDomain not found ");
        return 0;
    }

    @Override
    public boolean isDomainSwitch(ChatContext chatCtx, QueryContextReq searchCtx) {
        Long contextDomain = chatCtx.getParseInfo().getDomainId();
        Long currentDomain = searchCtx.getParseInfo().getDomainId();
        boolean noSwitch = currentDomain == null || contextDomain == null || contextDomain.equals(currentDomain);
        LOGGER.info("ChatContext isDomainSwitch [{}]", !noSwitch);
        return !noSwitch;
    }


    /**
     * to check can switch domain if context exit domain
     *
     * @return false will use context domain, true will use other domain , maybe include context domain
     */
    private static boolean isAllowSwitch(Map<Integer, SemanticQuery> domainQueryModes, SchemaMapInfo schemaMap,
            ChatContext chatCtx, QueryContextReq searchCtx, Integer domainId) {
        if (!Objects.nonNull(domainId) || domainId <= 0) {
            return true;
        }
        // except content domain, calculate the number of types for each domain, if numbers<=1 will not switch
        Map<Integer, SchemaElementCount> domainTypeMap = getDomainTypeMap(schemaMap);
        LOGGER.info("isAllowSwitch domainTypeMap [{}]", domainTypeMap);
        long otherDomainTypeNumBigOneCount = domainTypeMap.entrySet().stream()
                .filter(entry -> domainQueryModes.containsKey(entry.getKey()) && !entry.getKey().equals(domainId))
                .filter(entry -> entry.getValue().getCount() > 1).count();
        if (otherDomainTypeNumBigOneCount >= 1) {
            return true;
        }
        // if query text only contain time , will not switch
        if (searchCtx.getQueryText() != null && searchCtx.getParseInfo().getDateInfo() != null) {
            if (searchCtx.getParseInfo().getDateInfo().getText() != null) {
                if (searchCtx.getParseInfo().getDateInfo().getText().equalsIgnoreCase(searchCtx.getQueryText())) {
                    LOGGER.info("timeParseResults is not null , can not switch context , timeParseResults:{},",
                            searchCtx.getParseInfo().getDateInfo());
                    return false;
                }
            }
        }
        // if context domain not  in schemaMap , will switch
        if (schemaMap.getMatchedElements(domainId) == null || schemaMap.getMatchedElements(domainId).size() <= 0) {
            LOGGER.info("domainId not in schemaMap ");
            return true;
        }
        // other will not switch
        return false;
    }

    private static Map<Integer, SchemaElementCount> getDomainTypeMap(SchemaMapInfo schemaMap) {
        Map<Integer, SchemaElementCount> domainCount = new HashMap<>();
        for (Map.Entry<Integer, List<SchemaElementMatch>> entry : schemaMap.getDomainElementMatches().entrySet()) {
            List<SchemaElementMatch> schemaElementMatches = schemaMap.getMatchedElements(entry.getKey());
            if (schemaElementMatches != null && schemaElementMatches.size() > 0) {
                if (!domainCount.containsKey(entry.getKey())) {
                    domainCount.put(entry.getKey(), new SchemaElementCount());
                }
                SchemaElementCount schemaElementCount = domainCount.get(entry.getKey());
                Set<SchemaElementType> schemaElementTypes = new HashSet<>();
                schemaElementMatches.stream()
                        .forEach(schemaElementMatch -> schemaElementTypes.add(schemaElementMatch.getElementType()));
                SchemaElementMatch schemaElementMatchMax = schemaElementMatches.stream()
                        .sorted(ContextHelper.schemaElementMatchComparatorBySimilarity).findFirst().orElse(null);
                if (schemaElementMatchMax != null) {
                    schemaElementCount.setMaxSimilarity(schemaElementMatchMax.getSimilarity());
                }
                schemaElementCount.setCount(schemaElementTypes.size());

            }
        }
        return domainCount;
    }


}