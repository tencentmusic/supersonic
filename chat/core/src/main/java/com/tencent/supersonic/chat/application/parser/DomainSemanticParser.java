package com.tencent.supersonic.chat.application.parser;

import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.application.query.EntitySemanticQuery;
import com.tencent.supersonic.chat.application.query.MetricSemanticQuery;
import com.tencent.supersonic.chat.application.query.RuleSemanticQuery;
import com.tencent.supersonic.chat.application.query.RuleSemanticQueryManager;
import com.tencent.supersonic.chat.domain.pojo.chat.DomainInfos;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigResp;
import com.tencent.supersonic.chat.domain.service.ConfigService;
import com.tencent.supersonic.chat.domain.utils.ComponentFactory;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import com.tencent.supersonic.chat.domain.utils.DefaultMetricUtils;
import com.tencent.supersonic.chat.domain.utils.SchemaInfoConverter;
import com.tencent.supersonic.common.util.context.ContextUtils;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class DomainSemanticParser implements SemanticParser {

    private SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();

    @Override
    public void parse(QueryContextReq queryContext, ChatContext chatContext) {
        DomainInfos domainInfosDb = SchemaInfoConverter.convert(semanticLayer.getDomainSchemaInfo(new ArrayList<>()));
        Map<Integer, String> domainToName = domainInfosDb.getDomainToName();
        SchemaMapInfo mapInfo = queryContext.getMapInfo();

        // iterate all schemaElementMatches to resolve semantic query
        for (Integer domainId : mapInfo.getMatchedDomains()) {
            List<SchemaElementMatch> elementMatches = mapInfo.getMatchedElements(domainId);
            List<RuleSemanticQuery> queries = resolveQuery(elementMatches, queryContext);
            for (RuleSemanticQuery query : queries) {

                if (useBlackItem(query, domainId)) {
                    log.info("useBlackItem, skip query:{}", query);
                    continue;
                }
                addCandidateQuery(queryContext, chatContext, domainId.longValue(),
                        domainToName.get(domainId), query);
            }
        }

        // if no candidates have been found yet, count in chat context and try again
        if (queryContext.getCandidateQueries().size() <= 0) {
            if (chatContext.getParseInfo() != null && chatContext.getParseInfo().getDomainId() > 0) {
                Integer chatDomainId = Integer.valueOf(chatContext.getParseInfo().getDomainId().intValue());
                if (mapInfo.getMatchedDomains().contains(chatDomainId)) {
                    List<SchemaElementMatch> elementMatches = mapInfo.getMatchedElements(chatDomainId);

                    List<RuleSemanticQuery> queries = tryParseByContext(elementMatches, chatContext, queryContext);
                    for (RuleSemanticQuery query : queries) {
                        addCandidateQuery(queryContext, chatContext, chatDomainId.longValue(),
                                domainToName.get(chatDomainId), query);
                    }
                }
            }
        }
    }

    private boolean useBlackItem(RuleSemanticQuery query, Integer domainId) {
        if (Objects.isNull(domainId)) {
            return false;
        }
        ConfigService configService = ContextUtils.getBean(ConfigService.class);
        ChatConfigResp chatConfigResp = configService.fetchConfigByDomainId(domainId.longValue());
        if (Objects.nonNull(chatConfigResp) && Objects.nonNull(query) && Objects.nonNull(query.getParseInfo())) {
            List<SchemaElementMatch> elementMatches = query.getParseInfo().getElementMatches();
            if (!CollectionUtils.isEmpty(elementMatches)) {
                return useBlackItemInternal(elementMatches, chatConfigResp, query);

            }
        }
        return false;
    }

    private boolean useBlackItemInternal(List<SchemaElementMatch> elementMatches, ChatConfigResp chatConfigResp, RuleSemanticQuery query) {
        if (Objects.isNull(chatConfigResp)) {
            return false;
        }
        List<Long> blackDimIdList = new ArrayList<>();
        List<Long> blackMetricIdList = new ArrayList<>();
        if (query instanceof EntitySemanticQuery
                && Objects.nonNull(chatConfigResp.getChatDetailConfig())
                && Objects.nonNull(chatConfigResp.getChatDetailConfig().getVisibility())) {
            log.info("useBlackItem, handle EntitySemanticQuery blackList logic");
            blackDimIdList = chatConfigResp.getChatDetailConfig().getVisibility().getBlackDimIdList();
            blackMetricIdList = chatConfigResp.getChatDetailConfig().getVisibility().getBlackMetricIdList();
        }

        if (query instanceof MetricSemanticQuery
                && Objects.nonNull(chatConfigResp.getChatAggConfig())
                && Objects.nonNull(chatConfigResp.getChatAggConfig().getVisibility())) {
            log.info("useBlackItem, handle MetricSemanticQuery blackList logic");
            blackDimIdList = chatConfigResp.getChatAggConfig().getVisibility().getBlackDimIdList();
            blackMetricIdList = chatConfigResp.getChatAggConfig().getVisibility().getBlackMetricIdList();
        }
        return useBlackItemWithElementMatches(elementMatches, blackDimIdList, blackMetricIdList);
    }

    private boolean useBlackItemWithElementMatches(List<SchemaElementMatch> elementMatches, List<Long> blackDimIdList, List<Long> blackMetricIdList) {

        Set<Long> dimIds = elementMatches.stream()
                .filter(element -> SchemaElementType.VALUE.equals(element.getElementType()) || SchemaElementType.DIMENSION.equals(element.getElementType()))
                .map(element -> Long.valueOf(element.getElementID())).collect(Collectors.toSet());

        Set<Long> metricIds = elementMatches.stream()
                .filter(element -> SchemaElementType.METRIC.equals(element.getElementType()))
                .map(element -> Long.valueOf(element.getElementID())).collect(Collectors.toSet());


        return useBlackItemWithIds(dimIds, metricIds, blackDimIdList, blackMetricIdList);
    }

    private boolean useBlackItemWithIds(Set<Long> dimIds, Set<Long> metricIds, List<Long> blackDimIdList, List<Long> blackMetricIdList) {

        if (!CollectionUtils.isEmpty(blackDimIdList) && !CollectionUtils.isEmpty(dimIds)) {
            if (blackDimIdList.stream().anyMatch(dimIds::contains)) {
                log.info("useBlackItem, blackDimIdList:{}", blackDimIdList.stream().filter(dimIds::contains).collect(Collectors.toList()));
                return true;
            }
        }
        if (!CollectionUtils.isEmpty(blackMetricIdList) && !CollectionUtils.isEmpty(metricIds)) {
            if (blackMetricIdList.stream().anyMatch(metricIds::contains)) {
                log.info("useBlackItem, blackMetricIdList:{}", blackMetricIdList.stream().filter(metricIds::contains).collect(Collectors.toList()));
                return true;
            }
        }
        return false;
    }

    private void addCandidateQuery(QueryContextReq queryContext, ChatContext chatContext,
                                   Long domainId, String domainName, RuleSemanticQuery semanticQuery) {
        if (semanticQuery != null) {
            DefaultMetricUtils defaultMetricUtils = ContextUtils.getBean(DefaultMetricUtils.class);
            defaultMetricUtils.fillParseInfo(semanticQuery, domainId, domainName);
            inheritContext(semanticQuery, chatContext);
            defaultMetricUtils.fillDefaultMetric(semanticQuery.getParseInfo(), queryContext, chatContext);
            queryContext.getCandidateQueries().add(semanticQuery);
        }
    }

    protected void inheritContext(RuleSemanticQuery semanticQuery, ChatContext chatContext) {
        // is domain switch
        SemanticParseInfo semanticParse = semanticQuery.getParseInfo();
        DomainResolver domainResolver = ComponentFactory.getDomainResolver();
        if (!domainResolver.isDomainSwitch(chatContext, semanticParse)) {
            semanticQuery.inheritContext(chatContext);
        }
    }

    /**
     * try to add ChatContext to  SchemaMatch and look if match QueryMode
     *
     * @param elementMatches
     * @param chatCtx
     * @return
     */
    private List<RuleSemanticQuery> tryParseByContext(List<SchemaElementMatch> elementMatches,
                                                      ChatContext chatCtx, QueryContextReq queryCtx) {
        if (chatCtx.getParseInfo() != null && chatCtx.getParseInfo().getEntity() > 0) {
            Long entityCount = elementMatches.stream().filter(i -> SchemaElementType.ENTITY.equals(i.getElementType()))
                    .count();
            Long metricCount = elementMatches.stream().filter(i -> SchemaElementType.METRIC.equals(i.getElementType()))
                    .count();
            if (entityCount <= 0 && metricCount <= 0 && ContextHelper.hasEntityId(chatCtx)) {
                // try entity parse
                SchemaElementMatch entityElementMatch = SchemaElementMatch.builder()
                        .elementType(SchemaElementType.ENTITY).build();
                List<SchemaElementMatch> newSchemaMatches = new ArrayList<>();
                if (!CollectionUtils.isEmpty(elementMatches)) {
                    newSchemaMatches.addAll(elementMatches);
                }
                newSchemaMatches.add(entityElementMatch);
                List<RuleSemanticQuery> queries = doParseByContext(newSchemaMatches, chatCtx, queryCtx);
                if (queries.size() > 0) {
                    return queries;
                }
            }
        }
        return doParseByContext(elementMatches, chatCtx, queryCtx);
    }


    private List<RuleSemanticQuery> doParseByContext(List<SchemaElementMatch> elementMatches,
                                                     ChatContext chatCtx, QueryContextReq queryContext) {
        SemanticParseInfo contextSemanticParse = chatCtx.getParseInfo();
        if (contextSemanticParse != null) {
            List<SchemaElementMatch> newElementMatches = new ArrayList<>();
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

            for (List<SchemaElementType> schemaTypes : trySchemaElementTypes) {
                newElementMatches.clear();
                if (!CollectionUtils.isEmpty(elementMatches)) {
                    newElementMatches.addAll(elementMatches);
                }
                ContextHelper.mergeContextSchemaElementMatch(newElementMatches, elementMatches, schemaTypes,
                        contextSemanticParse);
                List<RuleSemanticQuery> queries = resolveQuery(newElementMatches, queryContext);
                if (queries.size() > 0) {
                    return queries;
                }
            }
        }
        return new ArrayList<>();
    }

    private List<RuleSemanticQuery> resolveQuery(List<SchemaElementMatch> candidateElementMatches,
                                                 QueryContextReq queryContext) {
        List<RuleSemanticQuery> matchedQueries = new ArrayList<>();
        for (RuleSemanticQuery semanticQuery : RuleSemanticQueryManager.getSemanticQueries()) {
            List<SchemaElementMatch> matches = semanticQuery.match(candidateElementMatches, queryContext);

            if (matches.size() > 0) {
                log.info("resolve match [{}:{}] ", semanticQuery.getQueryMode(), matches.size());
                RuleSemanticQuery query = RuleSemanticQueryManager.create(semanticQuery.getQueryMode());
                query.getParseInfo().getElementMatches().addAll(matches);
                matchedQueries.add(query);
            }
        }

        return matchedQueries;
    }
}
