package com.tencent.supersonic.chat.core.query.rule.metric;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.METRIC;
import static com.tencent.supersonic.chat.core.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.chat.core.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.common.pojo.Constants.DAY;
import static com.tencent.supersonic.common.pojo.Constants.DAY_FORMAT;
import static com.tencent.supersonic.common.pojo.Constants.DAY_FORMAT_INT;
import static com.tencent.supersonic.common.pojo.Constants.MONTH;
import static com.tencent.supersonic.common.pojo.Constants.MONTH_FORMAT;
import static com.tencent.supersonic.common.pojo.Constants.MONTH_FORMAT_INT;
import static com.tencent.supersonic.common.pojo.Constants.TIMES_FORMAT;
import static com.tencent.supersonic.common.pojo.Constants.TIME_FORMAT;
import static com.tencent.supersonic.common.pojo.Constants.WEEK;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.ChatDefaultConfigReq;
import com.tencent.supersonic.chat.api.pojo.response.AggregateInfo;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatDefaultRichConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.MetricInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.core.config.AggregatorConfig;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.chat.core.utils.QueryReqBuilder;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.RatioOverType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.api.response.QueryResultWithSchemaResp;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

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
        return super.match(candidateElementMatches, queryCtx);
    }

    @Override
    public void fillParseInfo(QueryContext queryContext, ChatContext chatContext) {
        super.fillParseInfo(queryContext, chatContext);

        parseInfo.setLimit(METRIC_MAX_RESULTS);
        if (parseInfo.getDateInfo() == null) {
            ChatConfigRichResp chatConfig = queryContext.getModelIdToChatRichConfig().get(parseInfo.getModelId());
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
            AggregateInfo aggregateInfo = getAggregateInfo(user, parseInfo, queryResp);
            queryResult.setAggregateInfo(aggregateInfo);
        }
    }

    public AggregateInfo getAggregateInfo(User user, SemanticParseInfo semanticParseInfo,
            QueryResultWithSchemaResp result) {
        AggregatorConfig aggregatorConfig = ContextUtils.getBean(AggregatorConfig.class);

        if (CollectionUtils.isEmpty(semanticParseInfo.getMetrics()) || !aggregatorConfig.getEnableRatio()) {
            return new AggregateInfo();
        }
        List<String> resultMetricNames = result.getColumns().stream().map(c -> c.getNameEn())
                .collect(Collectors.toList());
        Optional<SchemaElement> ratioMetric = semanticParseInfo.getMetrics().stream()
                .filter(m -> resultMetricNames.contains(m.getBizName())).findFirst();
        if (ratioMetric.isPresent()) {
            AggregateInfo aggregateInfo = new AggregateInfo();
            MetricInfo metricInfo = new MetricInfo();
            metricInfo.setStatistics(new HashMap<>());
            try {
                String dateField = QueryReqBuilder.getDateField(semanticParseInfo.getDateInfo());

                Optional<String> lastDayOp = result.getResultList().stream().filter(r -> r.containsKey(dateField))
                        .map(r -> r.get(dateField).toString())
                        .sorted(Comparator.reverseOrder()).findFirst();
                if (!lastDayOp.isPresent()) {
                    return new AggregateInfo();
                }
                Optional<Map<String, Object>> lastValue = result.getResultList().stream()
                        .filter(r -> r.get(dateField).toString().equals(lastDayOp.get())).findFirst();
                if (lastValue.isPresent() && lastValue.get().containsKey(ratioMetric.get().getBizName())) {
                    DecimalFormat df = new DecimalFormat("#.####");
                    metricInfo.setValue(df.format(lastValue.get().get(ratioMetric.get().getBizName())));
                }
                metricInfo.setDate(lastValue.get().get(dateField).toString());

                CompletableFuture<MetricInfo> metricInfoRoll = CompletableFuture
                        .supplyAsync(() -> {
                            return queryRatio(user, semanticParseInfo, ratioMetric.get(), AggOperatorEnum.RATIO_ROLL,
                                    result);
                        });
                CompletableFuture<MetricInfo> metricInfoOver = CompletableFuture
                        .supplyAsync(() -> {
                            return queryRatio(user, semanticParseInfo, ratioMetric.get(), AggOperatorEnum.RATIO_OVER,
                                    result);
                        });
                CompletableFuture.allOf(metricInfoRoll, metricInfoOver);
                metricInfo.setName(metricInfoRoll.get().getName());
                metricInfo.setValue(metricInfoRoll.get().getValue());
                metricInfo.getStatistics().putAll(metricInfoRoll.get().getStatistics());
                metricInfo.getStatistics().putAll(metricInfoOver.get().getStatistics());
                aggregateInfo.getMetricInfos().add(metricInfo);
            } catch (Exception e) {
                log.error("queryRatio error {}", e);
            }
            return aggregateInfo;
        }
        return new AggregateInfo();
    }

    private MetricInfo queryRatio(User user, SemanticParseInfo semanticParseInfo, SchemaElement metric,
            AggOperatorEnum aggOperatorEnum, QueryResultWithSchemaResp results) {
        MetricInfo metricInfo = new MetricInfo();
        metricInfo.setStatistics(new HashMap<>());
        QueryStructReq queryStructReq = QueryReqBuilder.buildStructRatioReq(semanticParseInfo, metric, aggOperatorEnum);
        DateConf dateInfo = semanticParseInfo.getDateInfo();
        String dateField = QueryReqBuilder.getDateField(dateInfo);

        queryStructReq.setGroups(new ArrayList<>(Arrays.asList(dateField)));
        queryStructReq.setDateInfo(getRatioDateConf(aggOperatorEnum, semanticParseInfo, results));

        QueryResultWithSchemaResp queryResp = semanticInterpreter.queryByStruct(queryStructReq, user);

        if (Objects.nonNull(queryResp) && !CollectionUtils.isEmpty(queryResp.getResultList())) {

            Map<String, Object> result = queryResp.getResultList().get(0);
            Optional<QueryColumn> valueColumn = queryResp.getColumns().stream()
                    .filter(c -> c.getNameEn().equals(metric.getBizName())).findFirst();
            if (valueColumn.isPresent()) {

                String valueField = String.format("%s_%s", valueColumn.get().getNameEn(),
                        aggOperatorEnum.getOperator());
                if (result.containsKey(valueColumn.get().getNameEn())) {
                    DecimalFormat df = new DecimalFormat("#.####");
                    metricInfo.setValue(df.format(result.get(valueColumn.get().getNameEn())));
                }
                String ratio = "";
                if (Objects.nonNull(result.get(valueField))) {
                    ratio = String.format("%.2f",
                            (Double.valueOf(result.get(valueField).toString()) * 100)) + "%";
                }
                String statisticsRollName = RatioOverType.DAY_ON_DAY.getShowName();
                String statisticsOverName = RatioOverType.WEEK_ON_DAY.getShowName();
                if (MONTH.equals(dateInfo.getPeriod())) {
                    statisticsRollName = RatioOverType.MONTH_ON_MONTH.getShowName();
                    statisticsOverName = RatioOverType.YEAR_ON_MONTH.getShowName();
                }
                if (WEEK.equals(dateInfo.getPeriod())) {
                    statisticsRollName = RatioOverType.WEEK_ON_WEEK.getShowName();
                    statisticsOverName = RatioOverType.MONTH_ON_WEEK.getShowName();
                }
                metricInfo.getStatistics().put(aggOperatorEnum.equals(AggOperatorEnum.RATIO_ROLL) ? statisticsRollName
                                : statisticsOverName,
                        ratio);
            }
            metricInfo.setName(metric.getName());
        }
        return metricInfo;
    }

    private DateConf getRatioDateConf(AggOperatorEnum aggOperatorEnum, SemanticParseInfo semanticParseInfo,
            QueryResultWithSchemaResp results) {
        String dateField = QueryReqBuilder.getDateField(semanticParseInfo.getDateInfo());
        Optional<String> lastDayOp = results.getResultList().stream()
                .map(r -> r.get(dateField).toString())
                .sorted(Comparator.reverseOrder()).findFirst();
        if (lastDayOp.isPresent()) {
            String lastDay = lastDayOp.get();
            DateConf dateConf = new DateConf();
            dateConf.setPeriod(semanticParseInfo.getDateInfo().getPeriod());
            dateConf.setDateMode(DateMode.LIST);
            List<String> dayList = new ArrayList<>();
            dayList.add(lastDay);
            String start = "";
            if (DAY.equalsIgnoreCase(semanticParseInfo.getDateInfo().getPeriod())) {
                DateTimeFormatter formatter = DateUtils.getDateFormatter(lastDay,
                        new String[]{DAY_FORMAT, DAY_FORMAT_INT});
                LocalDate end = LocalDate.parse(lastDay, formatter);
                start = aggOperatorEnum.equals(AggOperatorEnum.RATIO_ROLL) ? end.minusDays(1).format(formatter)
                        : end.minusWeeks(1).format(formatter);
            }
            if (WEEK.equalsIgnoreCase(semanticParseInfo.getDateInfo().getPeriod())) {
                DateTimeFormatter formatter = DateUtils.getTimeFormatter(lastDay,
                        new String[]{TIMES_FORMAT, DAY_FORMAT, TIME_FORMAT, DAY_FORMAT_INT});
                LocalDateTime end = LocalDateTime.parse(lastDay, formatter);
                start = aggOperatorEnum.equals(AggOperatorEnum.RATIO_ROLL) ? end.minusWeeks(1).format(formatter)
                        : end.minusMonths(1).with(DayOfWeek.MONDAY).format(formatter);
            }
            if (MONTH.equalsIgnoreCase(semanticParseInfo.getDateInfo().getPeriod())) {
                DateTimeFormatter formatter = DateUtils.getDateFormatter(lastDay,
                        new String[]{MONTH_FORMAT, MONTH_FORMAT_INT});
                YearMonth end = YearMonth.parse(lastDay, formatter);
                start = aggOperatorEnum.equals(AggOperatorEnum.RATIO_ROLL) ? end.minusMonths(1).format(formatter)
                        : end.minusYears(1).format(formatter);
            }
            dayList.add(start);
            dateConf.setDateList(dayList);
            return dateConf;

        }
        return semanticParseInfo.getDateInfo();
    }
}
