package com.tencent.supersonic.chat.application.parser;

import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.component.SemanticParser;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.application.query.*;
import com.tencent.supersonic.chat.domain.pojo.chat.DomainInfos;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import com.tencent.supersonic.chat.domain.utils.*;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.common.util.context.ContextUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
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
            Map<RuleSemanticQuery, List<SchemaElementMatch>> queryMatches = resolveQuery(elementMatches, queryContext);
            for (Map.Entry<RuleSemanticQuery, List<SchemaElementMatch>> match : queryMatches.entrySet()) {
                addCandidateQuery(queryContext, chatContext, domainId.longValue(),
                        domainToName.get(domainId), match.getKey(), match.getValue());
            }
        }

        // if no candidates have been found yet, count in chat context and try again
        if (queryContext.getCandidateQueries().size() <= 0) {
            if (chatContext.getParseInfo() != null && chatContext.getParseInfo().getDomainId() > 0) {
                Integer chatDomainId = Integer.valueOf(chatContext.getParseInfo().getDomainId().intValue());
                if (mapInfo.getMatchedDomains().contains(chatDomainId)) {
                    List<SchemaElementMatch> elementMatches = mapInfo.getMatchedElements(chatDomainId);
                    detectionContext(chatContext);
                    Map<RuleSemanticQuery, List<SchemaElementMatch>> queryMatches = tryParseByContext(elementMatches,
                            chatContext, queryContext);
                    for (Map.Entry<RuleSemanticQuery, List<SchemaElementMatch>> match : queryMatches.entrySet()) {
                        addCandidateQuery(queryContext, chatContext, chatDomainId.longValue(),
                                domainToName.get(chatDomainId), match.getKey(), match.getValue());
                    }
                }
            }
        }
    }

    private void addCandidateQuery(QueryContextReq queryContext, ChatContext chatContext,
            Long domainId, String domainName,
            RuleSemanticQuery semanticQuery, List<SchemaElementMatch> elementMatches) {
        if (semanticQuery != null) {
            fillParseInfo(semanticQuery, domainId, domainName, elementMatches);
            // inherit from context
            inheritContext(semanticQuery, chatContext);
            // default metric, date, dimension
            injectDefaultMetric(semanticQuery, queryContext, chatContext);
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

    protected void injectDefaultMetric(RuleSemanticQuery semanticQuery, QueryContextReq queryContext,
            ChatContext chatContext) {
        DefaultMetricUtils defaultMetricUtils = ContextUtils.getBean(DefaultMetricUtils.class);
        defaultMetricUtils.injectDefaultMetric(semanticQuery.getParseInfo(), queryContext, chatContext);
    }

    /**
     * get the chatContext for the tryParseByContext
     *
     * @param chatContext
     */
    protected void detectionContext(ChatContext chatContext) {
        if (chatContext.getParseInfo() != null) {
            SemanticParseInfo semanticParseInfo = chatContext.getParseInfo();
            if (semanticParseInfo.getQueryMode().equals(EntityDetail.QUERY_MODE)) {
                // EntityDetail model will unset some items
                semanticParseInfo.setDateInfo(null);
                semanticParseInfo.setMetrics(new HashSet<>());
                semanticParseInfo.setDimensions(new HashSet<>());
            }
        }
    }

    /**
     * try to add ChatContext to  SchemaElementMatch and look if match QueryMode
     *
     * @param elementMatches
     * @param chatCtx
     * @return
     */
    private Map<RuleSemanticQuery, List<SchemaElementMatch>> tryParseByContext(List<SchemaElementMatch> elementMatches,
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
                List<SchemaElementMatch> newSchemaElementMatch = new ArrayList<>();
                if (!CollectionUtils.isEmpty(elementMatches)) {
                    newSchemaElementMatch.addAll(elementMatches);
                }
                newSchemaElementMatch.add(entityElementMatch);
                Map<RuleSemanticQuery, List<SchemaElementMatch>> queryMatches = doParseByContext(newSchemaElementMatch,
                        chatCtx, queryCtx);
                if (queryMatches.size() > 0) {
                    return queryMatches;
                }
            }
        }
        return doParseByContext(elementMatches, chatCtx, queryCtx);
    }


    private Map<RuleSemanticQuery, List<SchemaElementMatch>> doParseByContext(List<SchemaElementMatch> elementMatches,
            ChatContext chatCtx, QueryContextReq queryCtx) {
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

            for (List<SchemaElementType> schemaElementTypes : trySchemaElementTypes) {
                newElementMatches.clear();
                if (!CollectionUtils.isEmpty(elementMatches)) {
                    newElementMatches.addAll(elementMatches);
                }
                ContextHelper.mergeContextSchemaElementMatch(newElementMatches, elementMatches, schemaElementTypes,
                        contextSemanticParse);
                Map<RuleSemanticQuery, List<SchemaElementMatch>> queryMatches = resolveQuery(newElementMatches,
                        queryCtx);
                if (queryMatches.size() > 0) {
                    return queryMatches;
                }
            }
        }
        return new HashMap<>();
    }

    private Map<RuleSemanticQuery, List<SchemaElementMatch>> resolveQuery(List<SchemaElementMatch> elementMatches,
            QueryContextReq queryCtx) {
        Map<RuleSemanticQuery, List<SchemaElementMatch>> matchMap = new HashMap<>();

        for (RuleSemanticQuery semanticQuery : RuleSemanticQueryManager.getSemanticQueries()) {
            List<SchemaElementMatch> matches = semanticQuery.match(elementMatches, queryCtx);

            if (matches.size() > 0) {
                log.info("resolve match [{}:{}] ", semanticQuery.getQueryMode(), matches.size());
                matchMap.put(RuleSemanticQueryManager.create(semanticQuery.getQueryMode()), matches);
            }
        }

        return matchMap;
    }

    public void fillParseInfo(SemanticQuery query, Long domainId, String domainName,
            List<SchemaElementMatch> elementMatches) {
        SemanticParseInfo parseInfo = query.getParseInfo();
        parseInfo.setDomainId(domainId);
        parseInfo.setDomainName(domainName);
        parseInfo.setQueryMode(query.getQueryMode());
        parseInfo.getElementMatches().addAll(elementMatches);

        DefaultSemanticInternalUtils defaultSemanticUtils = ContextUtils.getBean(DefaultSemanticInternalUtils.class);
        SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();

        DomainSchemaResp domainSchemaDesc = semanticLayer.getDomainSchemaInfo(parseInfo.getDomainId());
        ChatConfigRichInfo chaConfigRichDesc = defaultSemanticUtils.getChatConfigRichInfo(parseInfo.getDomainId());
        Map<Long, DimSchemaResp> dimensionDescMap = domainSchemaDesc.getDimensions().stream()
                .collect(Collectors.toMap(DimSchemaResp::getId, Function.identity()));
        Map<Long, MetricSchemaResp> metricDescMap = domainSchemaDesc.getMetrics().stream()
                .collect(Collectors.toMap(MetricSchemaResp::getId, Function.identity()));
        Map<Long, List<SchemaElementMatch>> dim2Values = new HashMap<>();

        for (SchemaElementMatch schemaElementMatch : elementMatches) {
            Long elementID = Long.valueOf(schemaElementMatch.getElementID());
            switch (schemaElementMatch.getElementType()) {
                case ID:
                case VALUE:
                    if (dimensionDescMap.containsKey(elementID)) {
                        if (dim2Values.containsKey(elementID)) {
                            dim2Values.get(elementID).add(schemaElementMatch);
                        } else {
                            dim2Values.put(elementID, new ArrayList<>(Arrays.asList(schemaElementMatch)));
                        }
                    }
                    break;
                case DIMENSION:
                    DimSchemaResp dimensionDesc = dimensionDescMap.get(elementID);
                    if (dimensionDesc != null) {
                        SchemaItem dimensionParseInfo = new SchemaItem();
                        dimensionParseInfo.setBizName(dimensionDesc.getBizName());
                        dimensionParseInfo.setName(dimensionDesc.getName());
                        dimensionParseInfo.setId(dimensionDesc.getId());
                        parseInfo.getDimensions().add(dimensionParseInfo);
                    }
                    break;
                case METRIC:
                    MetricSchemaResp metricDesc = metricDescMap.get(elementID);
                    if (metricDesc != null) {
                        SchemaItem metricItem = new SchemaItem();
                        metricItem.setBizName(metricDesc.getBizName());
                        metricItem.setName(metricDesc.getName());
                        metricItem.setId(metricDesc.getId());
                        metricItem.setCreatedAt(null);
                        metricItem.setUpdatedAt(null);
                        parseInfo.getMetrics().add(metricItem);
                    }
                    break;
                default:
            }
        }

        if (!dim2Values.isEmpty()) {
            for (Map.Entry<Long, List<SchemaElementMatch>> entry : dim2Values.entrySet()) {
                DimSchemaResp dimensionDesc = dimensionDescMap.get(entry.getKey());
                if (entry.getValue().size() == 1) {
                    SchemaElementMatch schemaElementMatch = entry.getValue().get(0);
                    Filter dimensionFilter = new Filter();
                    dimensionFilter.setValue(schemaElementMatch.getWord());
                    dimensionFilter.setBizName(dimensionDesc.getBizName());
                    dimensionFilter.setName(dimensionDesc.getName());
                    dimensionFilter.setOperator(FilterOperatorEnum.EQUALS);
                    dimensionFilter.setElementID(Long.valueOf(schemaElementMatch.getElementID()));
                    parseInfo.getDimensionFilters().add(dimensionFilter);
                    ContextHelper.setEntityId(entry.getKey(), schemaElementMatch.getWord(), chaConfigRichDesc,
                            parseInfo);
                } else {
                    Filter dimensionFilter = new Filter();
                    List<String> vals = new ArrayList<>();
                    entry.getValue().stream().forEach(i -> vals.add(i.getWord()));
                    dimensionFilter.setValue(vals);
                    dimensionFilter.setBizName(dimensionDesc.getBizName());
                    dimensionFilter.setName(dimensionDesc.getName());
                    dimensionFilter.setOperator(FilterOperatorEnum.IN);
                    dimensionFilter.setElementID(entry.getKey());
                    parseInfo.getDimensionFilters().add(dimensionFilter);
                }
            }
        }
    }
}