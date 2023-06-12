package com.tencent.supersonic.chat.application.parser;

import static java.time.LocalDate.now;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SemanticParser;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.chat.application.parser.resolver.DomainResolver;
import com.tencent.supersonic.chat.application.query.EntityDetail;
import com.tencent.supersonic.chat.application.query.EntityListFilter;
import com.tencent.supersonic.chat.application.query.EntityMetricFilter;
import com.tencent.supersonic.chat.application.query.MetricDomain;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import com.tencent.supersonic.chat.domain.pojo.config.DefaultMetric;
import com.tencent.supersonic.chat.domain.pojo.config.EntityRichInfo;
import com.tencent.supersonic.chat.domain.service.ChatService;
import com.tencent.supersonic.chat.domain.utils.DefaultSemanticInternalUtils;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.SchemaItem;
import com.tencent.supersonic.common.util.context.ContextUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class DefaultMetricSemanticParser implements SemanticParser {

    private final Logger logger = LoggerFactory.getLogger(DefaultMetricSemanticParser.class);
    private DomainResolver selectStrategy;
    private ChatService chatService;
    private DefaultSemanticInternalUtils defaultSemanticUtils;

    @Override
    public boolean parse(QueryContextReq queryContext, ChatContext chatCtx) {
        selectStrategy = ContextUtils.getBean(DomainResolver.class);
        chatService = ContextUtils.getBean(ChatService.class);
        defaultSemanticUtils = ContextUtils.getBean(DefaultSemanticInternalUtils.class);
        String queryMode = queryContext.getParseInfo().getQueryMode();
        if (StringUtils.isNotEmpty(queryMode)) {
            // QueryMode Selected
            if (!EntityListFilter.QUERY_MODE.equals(queryMode)) {
                Integer domainId = queryContext.getDomainId().intValue();

                List<SchemaElementMatch> matchedElements = queryContext.getMapInfo().getMatchedElements(domainId);
                if (!CollectionUtils.isEmpty(matchedElements)) {
                    long metricCount = matchedElements.stream()
                            .filter(schemaElementMatch -> schemaElementMatch.getElementType()
                                    .equals(SchemaElementType.METRIC)).count();
                    if (metricCount <= 0) {
                        if (chatCtx.getParseInfo() == null
                                || chatCtx.getParseInfo().getMetrics() == null
                                || chatCtx.getParseInfo().getMetrics().size() <= 0) {
                            logger.info("fillThemeDefaultMetricLogic");
                            fillThemeDefaultMetricLogic(queryContext.getParseInfo());
                        }
                    }
                }
                fillDateDomain(chatCtx, queryContext);
            }
        }
        defaultQueryMode(queryContext, chatCtx);

        if (EntityDetail.QUERY_MODE.equals(queryMode) || EntityMetricFilter.QUERY_MODE.equals(queryMode)) {
            addEntityDetailDimensionMetric(queryContext, chatCtx);
            dealNativeQuery(queryContext, true);
        }

        return false;
    }

    private void dealNativeQuery(QueryContextReq queryContext, boolean isNativeQuery) {
        if (Objects.nonNull(queryContext) && Objects.nonNull(queryContext.getParseInfo())) {
            queryContext.getParseInfo().setNativeQuery(isNativeQuery);
        }
    }

    private Set<String> addPrimaryDimension(EntityRichInfo entity, List<SchemaItem> dimensions) {
        Set<String> primaryDimensions = new HashSet<>();
        if (Objects.isNull(entity) || CollectionUtils.isEmpty(entity.getEntityIds())) {
            return primaryDimensions;
        }
        entity.getEntityIds().stream().forEach(dimSchemaDesc -> {
            SchemaItem dimension = new SchemaItem();
            BeanUtils.copyProperties(dimSchemaDesc, dimension);
            dimensions.add(dimension);
            primaryDimensions.add(dimSchemaDesc.getBizName());
        });
        return primaryDimensions;
    }

    protected void addEntityDetailDimensionMetric(QueryContextReq searchCtx, ChatContext chatCtx) {
        if (searchCtx.getParseInfo().getDomainId() > 0) {
            ChatConfigRichInfo chaConfigRichDesc = defaultSemanticUtils.getChatConfigRichInfo(
                    searchCtx.getParseInfo().getDomainId());
            if (chaConfigRichDesc != null) {
                SemanticParseInfo semanticParseInfo = searchCtx.getParseInfo();
                if (Objects.nonNull(semanticParseInfo) && CollectionUtils.isEmpty(semanticParseInfo.getDimensions())) {
                    List<SchemaItem> dimensions = new ArrayList<>();
                    List<SchemaItem> metrics = new ArrayList<>();
                    if (chaConfigRichDesc.getEntity() != null
                            && chaConfigRichDesc.getEntity().getEntityInternalDetailDesc() != null) {
                        chaConfigRichDesc.getEntity().getEntityInternalDetailDesc().getMetricList().stream()
                                .forEach(m -> metrics.add(getMetric(m)));
                        chaConfigRichDesc.getEntity().getEntityInternalDetailDesc().getDimensionList().stream()
                                .forEach(m -> dimensions.add(getDimension(m)));
                    }
                    semanticParseInfo.setDimensions(dimensions);
                    semanticParseInfo.setMetrics(metrics);
                }

            }
        }
    }

    protected void defaultQueryMode(QueryContextReq searchCtx, ChatContext chatCtx) {
        SchemaMapInfo schemaMap = searchCtx.getMapInfo();
        SemanticParseInfo parseInfo = searchCtx.getParseInfo();
        if (StringUtils.isEmpty(parseInfo.getQueryMode())) {
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
                        logger.info("defaultQueryMode [{}]", EntityDetail.QUERY_MODE);
                        parseInfo.setQueryMode(EntityDetail.QUERY_MODE);
                        parseInfo.setDomainId(domain);
                        return;
                    }
                    Long entityNUm = elementMatches.stream()
                            .filter(e -> e.getElementType().equals(SchemaElementType.ENTITY)).count();
                    if (filterNUm <= 0 && dimensionNUm <= 0 && entityNUm <= 0) {
                        // default as metric domain
                        if (metricrNUm > 0 || MetricDomain.QUERY_MODE.equals(queryMode)) {
                            // default as entity detail queryMode
                            logger.info("defaultQueryMode [{}]", MetricDomain.QUERY_MODE);
                            parseInfo.setQueryMode(MetricDomain.QUERY_MODE);
                            parseInfo.setDomainId(domain);
                            return;
                        }
                    }
                }
                if (CollectionUtils.isEmpty(schemaMap.getMatchedDomains()) && parseInfo != null
                        && parseInfo.getDateInfo() != null) {
                    // only query time
                    if (MetricDomain.QUERY_MODE.equals(queryMode)) {
                        // METRIC_DOMAIN context
                        logger.info("defaultQueryMode [{}]", MetricDomain.QUERY_MODE);
                        parseInfo.setQueryMode(MetricDomain.QUERY_MODE);
                        parseInfo.setDomainId(domain);
                        return;
                    }
                }
            }
        }
    }


    private void fillDateDomain(ChatContext chatCtx, QueryContextReq searchCtx) {
        SemanticParseInfo parseInfo = searchCtx.getParseInfo();

        if (parseInfo == null || parseInfo.getDateInfo() == null) {
            boolean isUpdateTime = false;
            if (selectStrategy.isDomainSwitch(chatCtx, searchCtx)) {
                isUpdateTime = true;
            }
            if (chatCtx.getParseInfo() == null
                    || chatCtx.getParseInfo().getDateInfo() == null) {
                isUpdateTime = true;
            }
            if (isUpdateTime && parseInfo != null && parseInfo.getDomainId() > 0) {
                logger.info("fillThemeDefaultTime");
                fillThemeDefaultTime(parseInfo.getDomainId(), parseInfo);
            }
        }
    }

    private void fillThemeDefaultMetricLogic(SemanticParseInfo semanticParseInfo) {
        ChatConfigRichInfo chaConfigRichDesc = defaultSemanticUtils.getChatConfigRichInfo(
                semanticParseInfo.getDomainId());

        if (Objects.isNull(chaConfigRichDesc) || CollectionUtils.isEmpty(chaConfigRichDesc.getDefaultMetrics())) {
            log.info("there is no defaultMetricIds info");
            return;
        }

        if (CollectionUtils.isEmpty(semanticParseInfo.getMetrics()) && CollectionUtils.isEmpty(
                semanticParseInfo.getDimensions())) {
            List<SchemaItem> metrics = new ArrayList<>();
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

    private void fillThemeDefaultTime(Long domain, SemanticParseInfo semanticParseInfo) {
        ChatConfigRichInfo chaConfigRichDesc = defaultSemanticUtils.getChatConfigRichInfo(
                semanticParseInfo.getDomainId());
        if (!Objects.isNull(chaConfigRichDesc) && !CollectionUtils.isEmpty(chaConfigRichDesc.getDefaultMetrics())) {
            DefaultMetric defaultMetricInfo = chaConfigRichDesc.getDefaultMetrics().get(0);
            DateConf dateInfo = new DateConf();
            dateInfo.setDateMode(DateConf.DateMode.RECENT_UNITS);
            dateInfo.setUnit(defaultMetricInfo.getUnit());
            dateInfo.setStartDate(now().minusDays(defaultMetricInfo.getUnit()).toString());
            dateInfo.setEndDate(now().minusDays(1).toString());
            dateInfo.setPeriod(defaultMetricInfo.getPeriod());
            semanticParseInfo.setDateInfo(dateInfo);
        }
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
