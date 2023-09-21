package com.tencent.supersonic.chat.query.rule.metric;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.METRIC;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.RelateSchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.request.ChatDefaultConfigReq;
import com.tencent.supersonic.chat.api.pojo.response.AggregateInfo;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatDefaultRichConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

@Slf4j
public abstract class MetricSemanticQuery extends RuleSemanticQuery {

    private static final Long METRIC_MAX_RESULTS = 365L;

    public MetricSemanticQuery() {
        super();
        queryMatcher.addOption(METRIC, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public List<SchemaElementMatch> match(List<SchemaElementMatch> candidateElementMatches,
                                          QueryContext queryCtx) {
        candidateElementMatches = filterElementMatches(candidateElementMatches);
        return super.match(candidateElementMatches, queryCtx);
    }

    private List<SchemaElementMatch> filterElementMatches(List<SchemaElementMatch> candidateElementMatches) {
        List<SchemaElementMatch> filteredMatches = new ArrayList<>();
        if (CollectionUtils.isEmpty(candidateElementMatches)
                || Objects.isNull(candidateElementMatches.get(0).getElement().getModel())) {
            return candidateElementMatches;
        }

        Long modelId = candidateElementMatches.get(0).getElement().getModel();
        ConfigService configService = ContextUtils.getBean(ConfigService.class);
        ChatConfigResp chatConfig = configService.fetchConfigByModelId(modelId);

        List<Long> blackDimIdList = new ArrayList<>();
        List<Long> blackMetricIdList = new ArrayList<>();
        if (Objects.nonNull(chatConfig.getChatAggConfig())
                && Objects.nonNull(chatConfig.getChatAggConfig().getVisibility())) {
            blackDimIdList = chatConfig.getChatAggConfig().getVisibility().getBlackDimIdList();
            blackMetricIdList = chatConfig.getChatAggConfig().getVisibility().getBlackMetricIdList();
        }

        for (SchemaElementMatch schemaElementMatch : candidateElementMatches) {
            SchemaElementType type = schemaElementMatch.getElement().getType();
            if (SchemaElementType.DIMENSION.equals(type) || SchemaElementType.VALUE.equals(type)) {
                if (!blackDimIdList.contains(schemaElementMatch.getElement().getId())) {
                    filteredMatches.add(schemaElementMatch);
                }
            } else if (SchemaElementType.METRIC.equals(type)) {
                if (!blackMetricIdList.contains(schemaElementMatch.getElement().getId())) {
                    filteredMatches.add(schemaElementMatch);
                }
            } else {
                filteredMatches.add(schemaElementMatch);
            }
        }
        filteredMatches = metricRelateDimensionCheck(filteredMatches, modelId);
        return filteredMatches;
    }

    private List<SchemaElementMatch> metricRelateDimensionCheck(List<SchemaElementMatch> elementMatches, Long modelId) {
        List<SchemaElementMatch> filterSchemaElementMatch = Lists.newArrayList();

        ModelSchema modelSchema = semanticInterpreter.getModelSchema(modelId, true);
        Set<SchemaElement> metricElements = modelSchema.getMetrics();
        Map<Long, SchemaElementMatch> valueElementMatchMap = elementMatches.stream()
                .filter(elementMatch ->
                        SchemaElementType.VALUE.equals(elementMatch.getElement().getType())
                        || SchemaElementType.ID.equals(elementMatch.getElement().getType()))
                .collect(Collectors.toMap(elementMatch -> elementMatch.getElement().getId(), e -> e, (e1, e2) -> e1));
        Map<Long, SchemaElement> metricMap = metricElements.stream()
                .collect(Collectors.toMap(SchemaElement::getId, e -> e, (e1, e2) -> e2));

        for (SchemaElementMatch schemaElementMatch : elementMatches) {
            if (!SchemaElementType.METRIC.equals(schemaElementMatch.getElement().getType())) {
                filterSchemaElementMatch.add(schemaElementMatch);
                continue;
            }
            SchemaElement metric = metricMap.get(schemaElementMatch.getElement().getId());
            if (metric == null) {
                continue;
            }
            List<RelateSchemaElement> relateSchemaElements = metric.getRelateSchemaElements();
            if (CollectionUtils.isEmpty(relateSchemaElements)) {
                filterSchemaElementMatch.add(schemaElementMatch);
                continue;
            }
            List<Long> necessaryDimensionIds = relateSchemaElements.stream()
                    .filter(RelateSchemaElement::isNecessary).map(RelateSchemaElement::getDimensionId)
                    .collect(Collectors.toList());
            boolean flag = true;
            for (Long necessaryDimensionId : necessaryDimensionIds) {
                if (!valueElementMatchMap.containsKey(necessaryDimensionId)) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                filterSchemaElementMatch.add(schemaElementMatch);
            }
        }
        return filterSchemaElementMatch;
    }

    @Override
    public void fillParseInfo(Long modelId, QueryContext queryContext, ChatContext chatContext) {
        super.fillParseInfo(modelId, queryContext, chatContext);

        parseInfo.setLimit(METRIC_MAX_RESULTS);
        if (parseInfo.getDateInfo() == null) {
            ConfigService configService = ContextUtils.getBean(ConfigService.class);
            ChatConfigRichResp chatConfig = configService.getConfigRichInfo(parseInfo.getModelId());
            ChatDefaultRichConfigResp defaultConfig = chatConfig.getChatAggRichConfig().getChatDefaultConfig();
            DateConf dateInfo = new DateConf();
            int unit = 1;
            if (Objects.nonNull(defaultConfig) && Objects.nonNull(defaultConfig.getUnit())) {
                unit = defaultConfig.getUnit();
            }
            String startDate = LocalDate.now().plusDays(-unit).toString();
            String endDate = startDate;

            if (ChatDefaultConfigReq.TimeMode.LAST.equals(defaultConfig.getTimeMode())) {
                dateInfo.setDateMode(DateConf.DateMode.BETWEEN);
            } else if (ChatDefaultConfigReq.TimeMode.RECENT.equals(defaultConfig.getTimeMode())) {
                dateInfo.setDateMode(DateConf.DateMode.RECENT);
                endDate = LocalDate.now().plusDays(-1).toString();
            }
            dateInfo.setUnit(unit);
            dateInfo.setPeriod(defaultConfig.getPeriod());
            dateInfo.setStartDate(startDate);
            dateInfo.setEndDate(endDate);

            parseInfo.setDateInfo(dateInfo);
        }
    }

    public void fillAggregateInfo(User user, QueryResult queryResult) {
        if (Objects.nonNull(queryResult)) {
            QueryResultWithSchemaResp queryResp = new QueryResultWithSchemaResp();
            queryResp.setColumns(queryResult.getQueryColumns());
            queryResp.setResultList(queryResult.getQueryResults());
            AggregateInfo aggregateInfo = ContextUtils.getBean(SemanticService.class)
                    .getAggregateInfo(user, parseInfo, queryResp);
            queryResult.setAggregateInfo(aggregateInfo);
        }
    }

}
