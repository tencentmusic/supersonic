package com.tencent.supersonic.chat.domain.utils;

import static java.time.LocalDate.now;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.application.parser.DomainResolver;
import com.tencent.supersonic.chat.application.query.EntityDetail;
import com.tencent.supersonic.chat.application.query.EntityListFilter;
import com.tencent.supersonic.chat.application.query.EntityListTopN;
import com.tencent.supersonic.chat.application.query.EntityMetricFilter;
import com.tencent.supersonic.chat.application.query.MetricDomain;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import com.tencent.supersonic.chat.domain.pojo.config.DefaultMetric;
import com.tencent.supersonic.chat.domain.pojo.config.EntityRichInfo;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.common.util.context.ContextUtils;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class DefaultMetricUtils {

    /**
     * supplementary default metric date dimension
     */
    public void injectDefaultMetric(SemanticParseInfo semanticParseInfo, QueryContextReq queryContext,
            ChatContext chatContext) {
        String queryMode = semanticParseInfo.getQueryMode();
        ChatConfigRichInfo chaConfigRichDesc = null;
        if (StringUtils.isNotEmpty(queryMode)) {
            if (semanticParseInfo == null) {
                return;
            }
            if (!EntityListFilter.QUERY_MODE.equals(queryMode)) {
                boolean isFillThemeDefaultMetricLogic = false;
                boolean isAddEntityDetailDimensionMetric = false;
                Integer domainId = queryContext.getDomainId().intValue();
                List<SchemaElementMatch> matchedElements = queryContext.getMapInfo().getMatchedElements(domainId);
                if (!CollectionUtils.isEmpty(matchedElements)) {
                    long metricCount = matchedElements.stream()
                            .filter(schemaElementMatch -> schemaElementMatch.getElementType()
                                    .equals(SchemaElementType.METRIC)).count();
                    if (metricCount <= 0) {
                        if (chatContext.getParseInfo() == null
                                || chatContext.getParseInfo().getMetrics() == null
                                || chatContext.getParseInfo().getMetrics().size() <= 0) {
                            log.info("fillThemeDefaultMetricLogic");
                            isFillThemeDefaultMetricLogic = true;
                        }
                    }
                } else {
                    log.info("fillThemeDefaultMetricLogic for empty matchedElements ");
                    isFillThemeDefaultMetricLogic = true;
                }
                if (EntityDetail.QUERY_MODE.equals(queryMode) || EntityMetricFilter.QUERY_MODE.equals(queryMode)) {
                    isAddEntityDetailDimensionMetric = true;
                    dealNativeQuery(semanticParseInfo, queryContext, true);
                }

                if (isFillThemeDefaultMetricLogic) {
                    if (chaConfigRichDesc == null) {
                        chaConfigRichDesc = getChatConfigRichInfo(semanticParseInfo.getDomainId());
                    }
                    fillThemeDefaultMetricLogic(semanticParseInfo, chaConfigRichDesc, queryContext);
                }
                if (isAddEntityDetailDimensionMetric) {
                    if (chaConfigRichDesc == null) {
                        chaConfigRichDesc = getChatConfigRichInfo(semanticParseInfo.getDomainId());
                    }
                    addEntityDetailDimensionMetric(semanticParseInfo, chaConfigRichDesc, queryContext, chatContext);
                }
                fillDateDomain(semanticParseInfo, chatContext, chaConfigRichDesc, queryContext);
            }
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
        if (Objects.isNull(entity) || CollectionUtils.isEmpty(entity.getEntityIds())) {
            return primaryDimensions;
        }
        entity.getEntityIds().stream().forEach(dimSchemaDesc -> {
            SchemaItem dimension = new SchemaItem();
            //BeanUtils.copyProperties(dimSchemaDesc, dimension);
            dimension.setName(dimSchemaDesc.getName());
            dimension.setBizName(dimSchemaDesc.getBizName());
            dimension.setId(dimSchemaDesc.getId());
            dimensions.add(dimension);
            primaryDimensions.add(dimSchemaDesc.getBizName());
        });
        return primaryDimensions;
    }

    public void addEntityTopDimension(SemanticParseInfo semanticParseInfo, ChatConfigRichInfo chaConfigRichDesc) {
        if (!semanticParseInfo.getQueryMode().equals(EntityListTopN.QUERY_MODE) || !semanticParseInfo.getDimensions()
                .isEmpty()) {
            return;
        }
        if (semanticParseInfo.getDomainId() > 0) {
            Long domainId = semanticParseInfo.getDomainId();
            if (chaConfigRichDesc == null) {
                chaConfigRichDesc = getChatConfigRichInfo(domainId);
            }
            if (chaConfigRichDesc != null && chaConfigRichDesc.getEntity() != null) {
                List<SchemaItem> dimensions = new ArrayList<>();
                addPrimaryDimension(chaConfigRichDesc.getEntity(), dimensions);
                semanticParseInfo.setDimensions(new HashSet<>(dimensions));
                semanticParseInfo.setLimit(1L);
            }
        }
    }

    public void addEntityDetailDimensionMetric(SemanticParseInfo semanticParseInfo,
            ChatConfigRichInfo chaConfigRichDesc, QueryContextReq queryContext,
            ChatContext chatCtx) {
        if (semanticParseInfo.getDomainId() > 0) {
            Long domainId = semanticParseInfo.getDomainId();

            if (chaConfigRichDesc != null) {
                if (chaConfigRichDesc.getEntity() == null
                        || chaConfigRichDesc.getEntity().getEntityInternalDetailDesc() == null) {
                    return;
                }

                List<SchemaElementMatch> schemaElementMatches = queryContext.getMapInfo()
                        .getMatchedElements(domainId.intValue());
                if (CollectionUtils.isEmpty(schemaElementMatches) || schemaElementMatches.stream()
                        .filter(s -> SchemaElementType.DIMENSION.equals(s.getElementType())).count() <= 0) {
                    log.info("addEntityDetailDimensionMetric catch");
                    Set<SchemaItem> dimensions = new LinkedHashSet();
                    chaConfigRichDesc.getEntity().getEntityInternalDetailDesc().getDimensionList().stream()
                            .forEach(m -> dimensions.add(getDimension(m)));
                    semanticParseInfo.setDimensions(dimensions);

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


    public void fillDateDomain(SemanticParseInfo parseInfo, ChatContext chatCtx, ChatConfigRichInfo chaConfigRichDesc,
            QueryContextReq queryContext) {
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
                fillThemeDefaultTime(parseInfo.getDomainId(), chaConfigRichDesc, parseInfo);
            }
        }
    }

    public void fillThemeDefaultMetricLogic(SemanticParseInfo semanticParseInfo, ChatConfigRichInfo chaConfigRichDesc,
            QueryContextReq queryContext) {
        //SemanticParseInfo semanticParseInfo = queryContext.getParseInfo();

        if (Objects.isNull(chaConfigRichDesc) || CollectionUtils.isEmpty(chaConfigRichDesc.getDefaultMetrics())) {
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
            chaConfigRichDesc.getDefaultMetrics().stream().forEach(metric -> {
                SchemaItem metricTmp = new SchemaItem();
                metricTmp.setId(metric.getMetricId());
                metricTmp.setBizName(metric.getBizName());
                metrics.add(metricTmp);
            });
            semanticParseInfo.setMetrics(metrics);
        }

        if (Objects.isNull(semanticParseInfo.getDateInfo()) || Objects.isNull(
                semanticParseInfo.getDateInfo().getDateMode())) {
            DefaultMetric defaultMetricInfo = chaConfigRichDesc.getDefaultMetrics().get(0);
            DateConf dateInfo = new DateConf();
            dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
            dateInfo.setUnit(defaultMetricInfo.getUnit());
            dateInfo.setPeriod(defaultMetricInfo.getPeriod());
            semanticParseInfo.setDateInfo(dateInfo);
        }

    }

    public void fillThemeDefaultTime(Long domain, ChatConfigRichInfo chaConfigRichDesc,
            SemanticParseInfo semanticParseInfo) {
        if (!Objects.isNull(semanticParseInfo.getDateInfo()) && !Objects.isNull(
                semanticParseInfo.getDateInfo().getDateMode())) {
            return;
        }
        if (chaConfigRichDesc == null) {
            chaConfigRichDesc = getChatConfigRichInfo(semanticParseInfo.getDomainId());
        }
        if (!Objects.isNull(chaConfigRichDesc) && !CollectionUtils.isEmpty(chaConfigRichDesc.getDefaultMetrics())) {
            DefaultMetric defaultMetricInfo = chaConfigRichDesc.getDefaultMetrics().get(0);
            DateConf dateInfo = new DateConf();
            dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
            dateInfo.setUnit(defaultMetricInfo.getUnit());
            dateInfo.setStartDate(now().minusDays(defaultMetricInfo.getUnit()).toString());
            dateInfo.setEndDate(now().minusDays(1).toString());
            dateInfo.setPeriod(defaultMetricInfo.getPeriod());
            semanticParseInfo.setDateInfo(dateInfo);
            log.info("fillThemeDefaultTime");
        }
    }

    public ChatConfigRichInfo getChatConfigRichInfo(Long domain) {
        DefaultSemanticInternalUtils defaultSemanticUtils = ContextUtils.getBean(DefaultSemanticInternalUtils.class);
        ChatConfigRichInfo chaConfigRichDesc = defaultSemanticUtils.getChatConfigRichInfo(domain);
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
