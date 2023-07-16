package com.tencent.supersonic.chat.domain.utils;

import static java.time.LocalDate.now;

import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.application.ConfigServiceImpl;
import com.tencent.supersonic.chat.application.parser.DomainResolver;
import com.tencent.supersonic.chat.application.query.*;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichResp;
import com.tencent.supersonic.chat.domain.pojo.config.ChatDefaultRichConfig;
import com.tencent.supersonic.chat.domain.pojo.config.EntityRichInfo;
import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class DefaultMetricUtils {

    @Autowired
    private ConfigServiceImpl configService;

    /**
     * supplementary default metric date dimension
     */
    public void fillDefaultMetric(SemanticParseInfo semanticParseInfo, QueryContextReq queryContext,
                                  ChatContext chatContext) {
        String queryMode = semanticParseInfo.getQueryMode();
        if (StringUtils.isNotEmpty(queryMode)) {
            Map<String, RuleSemanticQuery> semanticQuery = RuleSemanticQueryManager.getSemanticQueries().stream().collect(Collectors.toMap(RuleSemanticQuery::getQueryMode, Function.identity()));
            RuleSemanticQuery ruleSemanticQuery = semanticQuery.get(queryMode);
            if (semanticParseInfo == null) {
                return;
            }
//            if (!EntityListFilter.QUERY_MODE.equals(queryMode)) {
            boolean isFillAggDefaultMetricLogic = false;
            boolean isFillDetailDimensionMetric = false;
            Integer domainId = queryContext.getDomainId().intValue();
            ChatDefaultRichConfig chatDefaultConfig = null;
            Boolean isDetailMode = false;
            List<SchemaElementMatch> matchedElements = queryContext.getMapInfo().getMatchedElements(domainId);
            ChatConfigRichResp chaConfigRichDesc = getChatConfigRichInfo(semanticParseInfo.getDomainId());
            if (Objects.isNull(chaConfigRichDesc)) {
                return;
            }
            if (ruleSemanticQuery instanceof MetricSemanticQuery) {
                if (!CollectionUtils.isEmpty(matchedElements)) {
                    long metricCount = matchedElements.stream()
                            .filter(schemaElementMatch -> schemaElementMatch.getElementType()
                                    .equals(SchemaElementType.METRIC)).count();

                    if (metricCount <= 0) {
                        if (chatContext.getParseInfo() == null
                                || chatContext.getParseInfo().getMetrics() == null
                                || chatContext.getParseInfo().getMetrics().size() <= 0) {

                            log.info("isFillAggDefaultMetricLogic is true");
                            isFillAggDefaultMetricLogic = true;

                        }
                    }
                }
                if (Objects.nonNull(chaConfigRichDesc.getChatAggRichConfig())) {
                    chatDefaultConfig = chaConfigRichDesc.getChatAggRichConfig().getChatDefaultConfig();
                }

            } else if (ruleSemanticQuery instanceof EntitySemanticQuery) {
                log.info("fillThemeDefaultMetricLogic for empty matchedElements ");
                isFillDetailDimensionMetric = true;
                dealNativeQuery(semanticParseInfo, queryContext, true);
                isDetailMode = true;
                if (Objects.nonNull(chaConfigRichDesc.getChatDetailRichConfig())) {
                    chatDefaultConfig = chaConfigRichDesc.getChatDetailRichConfig().getChatDefaultConfig();
                }
            }


            if (isFillAggDefaultMetricLogic) {
                fillDefaultMetricAggLogic(semanticParseInfo, chaConfigRichDesc, queryContext);
            }

            if (isFillDetailDimensionMetric) {
                addEntityDetailDimensionMetric(semanticParseInfo, chaConfigRichDesc, queryContext, chatContext);
            }

            fillDateDomain(semanticParseInfo, chatContext, chaConfigRichDesc, chatDefaultConfig, isDetailMode);
//            }
            defaultQueryMode(semanticParseInfo, queryContext, chatContext);
            addEntityTopDimension(semanticParseInfo, chaConfigRichDesc);
        }
    }

    public void dealNativeQuery(SemanticParseInfo semanticParseInfo, QueryContextReq queryContext,
                                boolean isNativeQuery) {
        if (Objects.nonNull(queryContext) && Objects.nonNull(semanticParseInfo)) {
            semanticParseInfo.setNativeQuery(isNativeQuery);
        }
    }

    public Set<String> addPrimaryDimension(EntityRichInfo entity, List<SchemaItem> dimensions) {
        Set<String> primaryDimensions = new HashSet<>();
        if (Objects.isNull(entity) || Objects.isNull(entity.getDimItem())) {
            return primaryDimensions;
        }
        DimSchemaResp dimItem = entity.getDimItem();
        SchemaItem dimension = new SchemaItem();
        BeanUtils.copyProperties(dimItem, dimension);
        dimensions.add(dimension);
        primaryDimensions.add(dimItem.getBizName());
        return primaryDimensions;
    }

    public void addEntityTopDimension(SemanticParseInfo semanticParseInfo, ChatConfigRichResp chaConfigRichDesc) {
        if (!semanticParseInfo.getQueryMode().equals(EntityListTopN.QUERY_MODE) || !semanticParseInfo.getDimensions()
                .isEmpty()) {
            return;
        }
        if (semanticParseInfo.getDomainId() > 0) {
            Long domainId = semanticParseInfo.getDomainId();
            if (chaConfigRichDesc == null) {
                chaConfigRichDesc = getChatConfigRichInfo(domainId);
            }
            if (chaConfigRichDesc != null && chaConfigRichDesc.getChatDetailRichConfig() != null
                    && chaConfigRichDesc.getChatDetailRichConfig().getEntity() != null) {
                List<SchemaItem> dimensions = new ArrayList<>();
                addPrimaryDimension(chaConfigRichDesc.getChatDetailRichConfig().getEntity(), dimensions);
                semanticParseInfo.setDimensions(new HashSet<>(dimensions));
                semanticParseInfo.setLimit(1L);
            }
        }
    }

    public void addEntityDetailDimensionMetric(SemanticParseInfo semanticParseInfo, ChatConfigRichResp chaConfigRichDesc, QueryContextReq queryContext,
                                               ChatContext chatCtx) {
        if (semanticParseInfo.getDomainId() > 0) {
            Long domainId = semanticParseInfo.getDomainId();

            if (chaConfigRichDesc != null && chaConfigRichDesc.getChatDetailRichConfig() != null) {
                if (chaConfigRichDesc.getChatDetailRichConfig().getEntity() == null
                        || chaConfigRichDesc.getChatDetailRichConfig().getChatDefaultConfig() == null) {
                    return;
                }

                List<SchemaElementMatch> schemaElementMatches = queryContext.getMapInfo()
                        .getMatchedElements(domainId.intValue());
                if (CollectionUtils.isEmpty(schemaElementMatches)
                        || schemaElementMatches.stream().filter(s -> SchemaElementType.DIMENSION.equals(s.getElementType())).count() <= 0) {
                    log.info("addEntityDetailDimensionMetric catch");
                    if (CollectionUtils.isEmpty(semanticParseInfo.getDimensions())) {
                        Set<SchemaItem> dimensions = new LinkedHashSet();
                        List<SchemaItem> dimensionsConfig = chaConfigRichDesc.getChatDetailRichConfig().getChatDefaultConfig().getDimensions();
                        if (!CollectionUtils.isEmpty(dimensionsConfig)) {
                            dimensionsConfig.stream().forEach(m -> dimensions.add(m));
                        }
                        semanticParseInfo.setDimensions(dimensions);
                    }

                    if (CollectionUtils.isEmpty(semanticParseInfo.getMetrics())) {
                        Set<SchemaItem> metrics = new LinkedHashSet();
                        List<SchemaItem> metricsConfig = chaConfigRichDesc.getChatDetailRichConfig().getChatDefaultConfig().getMetrics();
                        if (!CollectionUtils.isEmpty(metricsConfig)) {
                            metricsConfig.stream().forEach(m -> metrics.add(m));
                        }
                        semanticParseInfo.setMetrics(metrics);
                    }
                }
            }
        }
    }

    public void defaultQueryMode(SemanticParseInfo semanticParseInfo, QueryContextReq queryContext,
                                 ChatContext chatCtx) {
        SchemaMapInfo schemaMap = queryContext.getMapInfo();
        if (StringUtils.isEmpty(semanticParseInfo.getQueryMode())) {
            if (chatCtx.getParseInfo() != null && chatCtx.getParseInfo().getDomainId() > 0) {
                //
                Long domain = chatCtx.getParseInfo().getDomainId();
                String queryMode = chatCtx.getParseInfo().getQueryMode();
                if (!CollectionUtils.isEmpty(schemaMap.getMatchedDomains()) && schemaMap.getMatchedDomains()
                        .contains(domain.intValue())) {
                    List<SchemaElementMatch> elementMatches = schemaMap.getMatchedElements(domain.intValue());
                    Long filterNUm = elementMatches.stream()
                            .filter(e -> e.getElementType().equals(SchemaElementType.VALUE) || e.getElementType()
                                    .equals(SchemaElementType.ID)).count();
                    Long dimensionNUm = elementMatches.stream()
                            .filter(e -> e.getElementType().equals(SchemaElementType.DIMENSION)).count();
                    Long metricrNUm = elementMatches.stream()
                            .filter(e -> e.getElementType().equals(SchemaElementType.METRIC)).count();
                    if (filterNUm > 0 && dimensionNUm > 0 && metricrNUm > 0) {
                        // default as entity detail queryMode
                        log.info("defaultQueryMode [{}]", EntityDetail.QUERY_MODE);
                        semanticParseInfo.setQueryMode(EntityDetail.QUERY_MODE);
                        semanticParseInfo.setDomainId(domain);
                        return;
                    }
                    Long entityNUm = elementMatches.stream()
                            .filter(e -> e.getElementType().equals(SchemaElementType.ENTITY)).count();
                    if (filterNUm <= 0 && dimensionNUm <= 0 && entityNUm <= 0) {
                        // default as metric domain
                        if (metricrNUm > 0 || MetricDomain.QUERY_MODE.equals(queryMode)) {
                            // default as entity detail queryMode
                            log.info("defaultQueryMode [{}]", MetricDomain.QUERY_MODE);
                            semanticParseInfo.setQueryMode(MetricDomain.QUERY_MODE);
                            semanticParseInfo.setDomainId(domain);
                            return;
                        }
                    }
                }
                if (CollectionUtils.isEmpty(schemaMap.getMatchedDomains()) && semanticParseInfo != null
                        && semanticParseInfo.getDateInfo() != null) {
                    // only query time
                    if (MetricDomain.QUERY_MODE.equals(queryMode)) {
                        // METRIC_DOMAIN context
                        log.info("defaultQueryMode [{}]", MetricDomain.QUERY_MODE);
                        semanticParseInfo.setQueryMode(MetricDomain.QUERY_MODE);
                        semanticParseInfo.setDomainId(domain);
                        return;
                    }
                }
            }
        }
    }

    public void fillParseInfo(SemanticQuery query, Long domainId, String domainName) {
        SemanticParseInfo parseInfo = query.getParseInfo();
        parseInfo.setDomainId(domainId);
        parseInfo.setDomainName(domainName);
        parseInfo.setQueryMode(query.getQueryMode());


        SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();

        DomainSchemaResp domainSchemaDesc = semanticLayer.getDomainSchemaInfo(parseInfo.getDomainId(), true);
        ChatConfigRichResp chaConfigRichDesc = configService.getConfigRichInfo(parseInfo.getDomainId());
        Map<Long, DimSchemaResp> dimensionDescMap = domainSchemaDesc.getDimensions().stream()
                .collect(Collectors.toMap(DimSchemaResp::getId, Function.identity()));
        Map<Long, MetricSchemaResp> metricDescMap = domainSchemaDesc.getMetrics().stream()
                .collect(Collectors.toMap(MetricSchemaResp::getId, Function.identity()));
        Map<Long, List<SchemaElementMatch>> dim2Values = new HashMap<>();

        List<SchemaElementMatch> elementMatches = query.getParseInfo().getElementMatches();
        for (SchemaElementMatch schemaMatch : elementMatches) {
            Long elementID = Long.valueOf(schemaMatch.getElementID());
            switch (schemaMatch.getElementType()) {
                case ID:
                case VALUE:
                    if (dimensionDescMap.containsKey(elementID)) {
                        if (dim2Values.containsKey(elementID)) {
                            dim2Values.get(elementID).add(schemaMatch);
                        } else {
                            dim2Values.put(elementID, new ArrayList<>(Arrays.asList(schemaMatch)));
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
                    SchemaElementMatch schemaMatch = entry.getValue().get(0);
                    Filter dimensionFilter = new Filter();
                    dimensionFilter.setValue(schemaMatch.getWord());
                    dimensionFilter.setBizName(dimensionDesc.getBizName());
                    dimensionFilter.setName(dimensionDesc.getName());
                    dimensionFilter.setOperator(FilterOperatorEnum.EQUALS);
                    dimensionFilter.setElementID(Long.valueOf(schemaMatch.getElementID()));
                    parseInfo.getDimensionFilters().add(dimensionFilter);
                    ContextHelper.setEntityId(entry.getKey(), schemaMatch.getWord(), chaConfigRichDesc,
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

    public void fillDateDomain(SemanticParseInfo parseInfo, ChatContext chatCtx, ChatConfigRichResp chaConfigRichDesc,
                               ChatDefaultRichConfig chatDefaultConfig, Boolean isDetailMode) {
        //SemanticParseInfo parseInfo = queryContext.getParseInfo();

        if (parseInfo == null || parseInfo.getDateInfo() == null) {
            DomainResolver selectStrategy = ComponentFactory.getDomainResolver();
            boolean isUpdateTime = false;
            if (selectStrategy.isDomainSwitch(chatCtx, parseInfo)) {
                isUpdateTime = true;
            }
            if (chatCtx.getParseInfo() == null
                    || chatCtx.getParseInfo().getDateInfo() == null) {
                isUpdateTime = true;
            }
            if (isUpdateTime && parseInfo != null && parseInfo.getDomainId() > 0) {

                fillThemeDefaultTime(chaConfigRichDesc, parseInfo, chatDefaultConfig, isDetailMode);
            }
        }
    }

    public void fillDefaultMetricAggLogic(SemanticParseInfo semanticParseInfo, ChatConfigRichResp chaConfigRichDesc, QueryContextReq queryContext) {
        //SemanticParseInfo semanticParseInfo = queryContext.getParseInfo();

        if (Objects.isNull(chaConfigRichDesc) || Objects.isNull(chaConfigRichDesc.getChatAggRichConfig())
                || Objects.isNull(chaConfigRichDesc.getChatAggRichConfig().getChatDefaultConfig())
                || CollectionUtils.isEmpty(chaConfigRichDesc.getChatAggRichConfig().getChatDefaultConfig().getMetrics())) {
            log.info("there is no defaultMetricIds info");
            return;
        }

        if (queryContext.getMapInfo() == null || !queryContext.getMapInfo().getMatchedDomains()
                .contains(chaConfigRichDesc.getDomainId().intValue())) {
            return;
        }
        List<SchemaElementMatch> schemaElementMatches = queryContext.getMapInfo()
                .getMatchedElements(chaConfigRichDesc.getDomainId().intValue());
        long metricNum = schemaElementMatches.stream().filter(e -> e.getElementType().equals(SchemaElementType.METRIC))
                .count();
        long dimensionNum = schemaElementMatches.stream()
                .filter(e -> e.getElementType().equals(SchemaElementType.DIMENSION)).count();
        if (metricNum <= 0 && dimensionNum <= 0) {
            Set<SchemaItem> metrics = new LinkedHashSet();
            chaConfigRichDesc.getChatAggRichConfig().getChatDefaultConfig().getMetrics().stream().forEach(metric -> {
                SchemaItem metricTmp = new SchemaItem();
                metricTmp.setId(metric.getId());
                metricTmp.setBizName(metric.getBizName());
                metrics.add(metricTmp);
            });
            semanticParseInfo.setMetrics(metrics);
        }

        if (Objects.isNull(semanticParseInfo.getDateInfo()) || Objects.isNull(
                semanticParseInfo.getDateInfo().getDateMode())) {
            ChatDefaultRichConfig chatDefaultConfig = chaConfigRichDesc.getChatAggRichConfig().getChatDefaultConfig();
            DateConf dateInfo = new DateConf();
            dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
            dateInfo.setUnit(chatDefaultConfig.getUnit());
            dateInfo.setPeriod(chatDefaultConfig.getPeriod());
            semanticParseInfo.setDateInfo(dateInfo);
        }

    }

    public void fillThemeDefaultTime(ChatConfigRichResp chaConfigRichDesc, SemanticParseInfo semanticParseInfo, ChatDefaultRichConfig chatDefaultConfig, Boolean isDetailMode) {
        if (!Objects.isNull(semanticParseInfo.getDateInfo()) && !Objects.isNull(
                semanticParseInfo.getDateInfo().getDateMode())) {
            return;
        }
        if (chaConfigRichDesc == null) {
            chaConfigRichDesc = getChatConfigRichInfo(semanticParseInfo.getDomainId());
        }
        if (!Objects.isNull(chaConfigRichDesc) && Objects.nonNull(chatDefaultConfig) && !CollectionUtils.isEmpty(chatDefaultConfig.getMetrics())) {
            DateConf dateInfo = new DateConf();
            if (isDetailMode) {
                dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
                dateInfo.setUnit(1);
                dateInfo.setPeriod(Constants.DAY);
            } else {
                dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
                dateInfo.setUnit(chatDefaultConfig.getUnit());
                dateInfo.setPeriod(chatDefaultConfig.getPeriod());
            }


            semanticParseInfo.setDateInfo(dateInfo);
            log.info("fillThemeDefaultTime");
        }
    }

    public ChatConfigRichResp getChatConfigRichInfo(Long domain) {
        ChatConfigRichResp chaConfigRichDesc = configService.getConfigRichInfo(domain);
        return chaConfigRichDesc;
    }

    private SchemaItem getMetric(MetricSchemaResp metricSchemaDesc) {
        SchemaItem queryMeta = new SchemaItem();
        queryMeta.setId(metricSchemaDesc.getId());
        queryMeta.setBizName(metricSchemaDesc.getBizName());
        queryMeta.setName(metricSchemaDesc.getName());
        return queryMeta;
    }

    private SchemaItem getDimension(DimSchemaResp dimSchemaDesc) {
        SchemaItem queryMeta = new SchemaItem();
        queryMeta.setId(dimSchemaDesc.getId());
        queryMeta.setBizName(dimSchemaDesc.getBizName());
        queryMeta.setName(dimSchemaDesc.getName());
        return queryMeta;
    }
}
