package com.tencent.supersonic.chat.query.rule.metric;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.METRIC;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
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
import java.util.Objects;
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
        return filteredMatches;
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
