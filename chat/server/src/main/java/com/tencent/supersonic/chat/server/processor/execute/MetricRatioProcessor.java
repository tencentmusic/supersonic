package com.tencent.supersonic.chat.server.processor.execute;

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
import com.tencent.supersonic.chat.server.pojo.ChatExecuteContext;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.DateConf.DateMode;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.RatioOverType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.AggregateInfo;
import com.tencent.supersonic.headless.api.pojo.MetricInfo;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.config.AggregatorConfig;
import com.tencent.supersonic.headless.chat.utils.QueryReqBuilder;
import com.tencent.supersonic.headless.server.service.SemanticLayerService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * Add ratio queries for metric queries.
 */
@Slf4j
public class MetricRatioProcessor implements ExecuteResultProcessor {

    @Override
    public void process(ChatExecuteContext chatExecuteContext, QueryResult queryResult) {
        SemanticParseInfo semanticParseInfo = chatExecuteContext.getParseInfo();
        AggregatorConfig aggregatorConfig = ContextUtils.getBean(AggregatorConfig.class);
        if (CollectionUtils.isEmpty(semanticParseInfo.getMetrics())
                || !aggregatorConfig.getEnableRatio()
                || !QueryType.METRIC.equals(semanticParseInfo.getQueryType())) {
            return;
        }
        AggregateInfo aggregateInfo = getAggregateInfo(chatExecuteContext.getUser(),
                semanticParseInfo, queryResult);
        queryResult.setAggregateInfo(aggregateInfo);
    }

    public AggregateInfo getAggregateInfo(User user, SemanticParseInfo semanticParseInfo, QueryResult queryResult) {

        Set<String> resultMetricNames = new HashSet<>();
        queryResult.getQueryColumns()
                .stream().forEach(c -> resultMetricNames.addAll(SqlSelectHelper.getColumnFromExpr(c.getNameEn())));
        Optional<SchemaElement> ratioMetric = semanticParseInfo.getMetrics().stream()
                .filter(m -> resultMetricNames.contains(m.getBizName())).findFirst();

        AggregateInfo aggregateInfo = new AggregateInfo();
        if (!ratioMetric.isPresent()) {
            return aggregateInfo;
        }

        try {
            String dateField = QueryReqBuilder.getDateField(semanticParseInfo.getDateInfo());
            Optional<String> lastDayOp = queryResult.getQueryResults().stream()
                    .filter(r -> r.containsKey(dateField))
                    .map(r -> r.get(dateField).toString())
                    .sorted(Comparator.reverseOrder()).findFirst();

            if (!lastDayOp.isPresent()) {
                return new AggregateInfo();
            }
            Optional<Map<String, Object>> lastValue = queryResult.getQueryResults().stream()
                    .filter(r -> r.get(dateField).toString().equals(lastDayOp.get())).findFirst();

            MetricInfo metricInfo = new MetricInfo();
            metricInfo.setStatistics(new HashMap<>());
            if (lastValue.isPresent() && lastValue.get().containsKey(ratioMetric.get().getBizName())) {
                DecimalFormat df = new DecimalFormat("#.####");
                metricInfo.setValue(df.format(lastValue.get().get(ratioMetric.get().getBizName())));
            }
            metricInfo.setDate(lastValue.get().get(dateField).toString());

            CompletableFuture<MetricInfo> metricInfoRoll = CompletableFuture.supplyAsync(
                    () -> queryRatio(user, semanticParseInfo, ratioMetric.get(), AggOperatorEnum.RATIO_ROLL,
                            queryResult));
            CompletableFuture<MetricInfo> metricInfoOver = CompletableFuture.supplyAsync(
                    () -> queryRatio(user, semanticParseInfo, ratioMetric.get(), AggOperatorEnum.RATIO_OVER,
                            queryResult));
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

    @SneakyThrows
    private MetricInfo queryRatio(User user, SemanticParseInfo semanticParseInfo, SchemaElement metric,
            AggOperatorEnum aggOperatorEnum, QueryResult queryResult) {

        QueryStructReq queryStructReq = QueryReqBuilder.buildStructRatioReq(semanticParseInfo, metric, aggOperatorEnum);
        String dateField = QueryReqBuilder.getDateField(semanticParseInfo.getDateInfo());
        queryStructReq.setGroups(new ArrayList<>(Arrays.asList(dateField)));
        queryStructReq.setDateInfo(getRatioDateConf(aggOperatorEnum, semanticParseInfo, queryResult));
        queryStructReq.setConvertToSql(false);
        SemanticLayerService queryService = ContextUtils.getBean(SemanticLayerService.class);
        SemanticQueryResp queryResp = queryService.queryByReq(queryStructReq, user);
        MetricInfo metricInfo = new MetricInfo();
        metricInfo.setStatistics(new HashMap<>());
        if (Objects.isNull(queryResp) || CollectionUtils.isEmpty(queryResp.getResultList())) {
            return metricInfo;
        }

        Map<String, Object> result = queryResp.getResultList().get(0);
        Optional<QueryColumn> valueColumn = queryResp.getColumns().stream()
                .filter(c -> c.getNameEn().equals(metric.getBizName())).findFirst();

        if (!valueColumn.isPresent()) {
            return metricInfo;
        }
        String valueField = String.format("%s_%s", valueColumn.get().getNameEn(), aggOperatorEnum.getOperator());
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
        if (MONTH.equals(semanticParseInfo.getDateInfo().getPeriod())) {
            statisticsRollName = RatioOverType.MONTH_ON_MONTH.getShowName();
            statisticsOverName = RatioOverType.YEAR_ON_MONTH.getShowName();
        }
        if (WEEK.equals(semanticParseInfo.getDateInfo().getPeriod())) {
            statisticsRollName = RatioOverType.WEEK_ON_WEEK.getShowName();
            statisticsOverName = RatioOverType.MONTH_ON_WEEK.getShowName();
        }
        metricInfo.getStatistics().put(aggOperatorEnum.equals(AggOperatorEnum.RATIO_ROLL)
                ? statisticsRollName : statisticsOverName, ratio);
        metricInfo.setName(metric.getName());
        return metricInfo;
    }

    private DateConf getRatioDateConf(AggOperatorEnum aggOperatorEnum, SemanticParseInfo semanticParseInfo,
            QueryResult queryResult) {
        String dateField = QueryReqBuilder.getDateField(semanticParseInfo.getDateInfo());

        Optional<String> lastDayOp = queryResult.getQueryResults()
                .stream().map(r -> r.get(dateField).toString())
                .sorted(Comparator.reverseOrder()).findFirst();

        if (!lastDayOp.isPresent()) {
            return semanticParseInfo.getDateInfo();
        }
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

}
