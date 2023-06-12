package com.tencent.supersonic.chat.application.parser;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SemanticLayer;
import com.tencent.supersonic.chat.api.service.SemanticParser;
import com.tencent.supersonic.chat.api.service.SemanticQuery;
import com.tencent.supersonic.chat.application.parser.resolver.DomainResolver;
import com.tencent.supersonic.chat.application.parser.resolver.SemanticQueryResolver;
import com.tencent.supersonic.chat.domain.pojo.semantic.DomainInfos;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import com.tencent.supersonic.chat.domain.utils.SchemaInfoConverter;
import com.tencent.supersonic.common.util.context.ContextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class DomainSemanticParser implements SemanticParser {

    private final Logger logger = LoggerFactory.getLogger(DomainSemanticParser.class);
    private DomainResolver domainResolver;

    private SemanticQueryResolver semanticQueryResolver;

    @Override
    public boolean parse(QueryContextReq queryContext, ChatContext chatCtx) {
        DomainInfos domainInfosDb = SchemaInfoConverter.convert(
                ContextUtils.getBean(SemanticLayer.class).getDomainSchemaInfo(new ArrayList<>()));
        Map<Integer, String> domainToName = domainInfosDb.getDomainToName();

        SchemaMapInfo mapInfo = queryContext.getMapInfo();
        SemanticParseInfo parseInfo = queryContext.getParseInfo();

        domainResolver = ContextUtils.getBean(DomainResolver.class);
        semanticQueryResolver = ContextUtils.getBean(SemanticQueryResolver.class);

        Map<Integer, SemanticQuery> domainSemanticQuery = new HashMap<>();
        // Round 1: find all domains that can be resolved to any query mode

        for (Integer domain : mapInfo.getMatchedDomains()) {
            List<SchemaElementMatch> elementMatches = mapInfo.getMatchedElements(domain);

            SemanticQuery query = semanticQueryResolver.resolve(elementMatches, queryContext);

            if (Objects.nonNull(query)) {
                domainSemanticQuery.put(domain, query);
            }
        }
        // only one domain is found, no need to rank
        if (domainSemanticQuery.size() == 1) {
            Optional<Map.Entry<Integer, SemanticQuery>> match = domainSemanticQuery.entrySet().stream().findFirst();
            if (match.isPresent()) {
                logger.info("select by only one [{}:{}]", match.get().getKey(), match.get().getValue());
                parseInfo.setDomainId(Long.valueOf(match.get().getKey()));
                parseInfo.setDomainName(domainToName.get(Integer.valueOf(match.get().getKey())));
                parseInfo.setQueryMode(match.get().getValue().getQueryMode());
                return false;
            }
        } else if (domainSemanticQuery.size() > 1) {
            // will choose one by the domain select
            Integer domainId = domainResolver.resolve(domainSemanticQuery, queryContext, chatCtx, mapInfo);
            if (domainId > 0) {
                Map.Entry<Integer, SemanticQuery> match = domainSemanticQuery.entrySet().stream()
                        .filter(entry -> entry.getKey().equals(domainId)).findFirst().orElse(null);
                logger.info("select by selectStrategy [{}:{}]", domainId, match.getValue());
                parseInfo.setDomainId(Long.valueOf(match.getKey()));
                parseInfo.setDomainName(domainToName.get(Integer.valueOf(match.getKey())));
                parseInfo.setQueryMode(match.getValue().getQueryMode());
                return false;
            }
        }
        // Round 2: no domains can be found yet, count in chat context
        if (chatCtx.getParseInfo() != null && chatCtx.getParseInfo().getDomainId() > 0) {
            Integer chatDomain = Integer.valueOf(chatCtx.getParseInfo().getDomainId().intValue());
            if (mapInfo.getMatchedDomains().contains(chatDomain) || CollectionUtils.isEmpty(
                    mapInfo.getMatchedDomains())) {
                List<SchemaElementMatch> elementMatches = mapInfo.getMatchedElements(chatDomain);
                if (CollectionUtils.isEmpty(elementMatches)) {
                    parseInfo.setDomainId(Long.valueOf(chatDomain));
                    parseInfo.setDomainName(domainToName.get(chatDomain));
                    parseInfo.setQueryMode(chatCtx.getParseInfo().getQueryMode());
                    return false;
                }

                SemanticQuery query = tryParseByContext(elementMatches, chatCtx, queryContext);
                if (Objects.nonNull(query)) {
                    logger.info("select by context count  [{}:{}]", chatDomain, query);
                    parseInfo.setDomainId(Long.valueOf(chatDomain));
                    parseInfo.setDomainName(domainToName.get(chatDomain));
                    parseInfo.setQueryMode(query.getQueryMode());
                    return false;
                }
            }
        }
        // Round 3: no domains can be found yet, count in default metric
        return false;
    }


    /**
     * try to add ChatContext to  SchemaElementMatch and look if match QueryMode
     *
     * @param elementMatches
     * @param chatCtx
     * @return
     */
    private SemanticQuery tryParseByContext(List<SchemaElementMatch> elementMatches, ChatContext chatCtx,
            QueryContextReq searchCt) {
        if (chatCtx.getParseInfo() != null && chatCtx.getParseInfo().getEntity() > 0) {
            Long entityCount = elementMatches.stream().filter(i -> SchemaElementType.ENTITY.equals(i.getElementType()))
                    .count();
            Long metricCount = elementMatches.stream().filter(i -> SchemaElementType.METRIC.equals(i.getElementType()))
                    .count();
            if (entityCount <= 0 && metricCount <= 0 && ContextHelper.hasEntityId(chatCtx)) {
                // try entity parse
                SchemaElementMatch entityElementMatch = new SchemaElementMatch();
                entityElementMatch.setElementType(SchemaElementType.ENTITY);
                List<SchemaElementMatch> newSchemaElementMatch = new ArrayList<>();
                if (!CollectionUtils.isEmpty(elementMatches)) {
                    newSchemaElementMatch.addAll(elementMatches);
                }
                newSchemaElementMatch.add(entityElementMatch);
                SemanticQuery semanticQuery = doParseByContext(newSchemaElementMatch, chatCtx, searchCt);
                if (Objects.nonNull(semanticQuery)) {
                    return semanticQuery;
                }
            }
        }
        return doParseByContext(elementMatches, chatCtx, searchCt);
    }

    private SemanticQuery doParseByContext(List<SchemaElementMatch> elementMatches, ChatContext chatCtx,
            QueryContextReq searchCt) {
        SemanticParseInfo contextSemanticParseInfo = chatCtx.getParseInfo();
        if (contextSemanticParseInfo != null) {
            List<SchemaElementMatch> newSchemaElementMatch = new ArrayList<>();
            List<List<SchemaElementType>> trySchemaElementTypes = new LinkedList<>();
            // try DIMENSION+METRIC+VALUE
            // try DIMENSION+METRIC METRIC+VALUE  DIMENSION+VALUE
            // try DIMENSION METRIC VALUE single
            trySchemaElementTypes.add(new ArrayList<>(
                    Arrays.asList(SchemaElementType.DIMENSION, SchemaElementType.METRIC, SchemaElementType.VALUE)));
            trySchemaElementTypes.add(
                    new ArrayList<>(Arrays.asList(SchemaElementType.METRIC, SchemaElementType.VALUE)));
            trySchemaElementTypes.add(
                    new ArrayList<>(Arrays.asList(SchemaElementType.DIMENSION, SchemaElementType.METRIC)));
            trySchemaElementTypes.add(
                    new ArrayList<>(Arrays.asList(SchemaElementType.DIMENSION, SchemaElementType.VALUE)));
            trySchemaElementTypes.add(new ArrayList<>(Arrays.asList(SchemaElementType.METRIC)));
            trySchemaElementTypes.add(new ArrayList<>(Arrays.asList(SchemaElementType.VALUE)));
            trySchemaElementTypes.add(new ArrayList<>(Arrays.asList(SchemaElementType.DIMENSION)));

            for (List<SchemaElementType> schemaElementTypes : trySchemaElementTypes) {
                newSchemaElementMatch.clear();
                if (!CollectionUtils.isEmpty(elementMatches)) {
                    newSchemaElementMatch.addAll(elementMatches);
                }
                ContextHelper.mergeContextSchemaElementMatch(newSchemaElementMatch, elementMatches, schemaElementTypes,
                        contextSemanticParseInfo);
                SemanticQuery semanticQuery = semanticQueryResolver.resolve(newSchemaElementMatch, searchCt);
                if (semanticQuery != null) {
                    return semanticQuery;
                }
            }
        }
        return null;
    }
}
