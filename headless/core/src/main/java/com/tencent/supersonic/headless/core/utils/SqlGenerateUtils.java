package com.tencent.supersonic.headless.core.utils;

import static com.tencent.supersonic.common.pojo.Constants.DAY;
import static com.tencent.supersonic.common.pojo.Constants.DAY_FORMAT;
import static com.tencent.supersonic.common.pojo.Constants.JOIN_UNDERLINE;
import static com.tencent.supersonic.common.pojo.Constants.MONTH;
import static com.tencent.supersonic.common.pojo.Constants.UNDERLINE;
import static com.tencent.supersonic.common.pojo.Constants.WEEK;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.DateModeUtils;
import com.tencent.supersonic.common.util.SqlFilterUtils;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricSchemaResp;
import com.tencent.supersonic.headless.core.config.ExecutorConfig;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * tools functions to analyze queryStructReq
 */
@Component
@Slf4j
public class SqlGenerateUtils {

    private final SqlFilterUtils sqlFilterUtils;

    private final DateModeUtils dateModeUtils;

    private final ExecutorConfig executorConfig;

    public SqlGenerateUtils(SqlFilterUtils sqlFilterUtils,
            DateModeUtils dateModeUtils, ExecutorConfig executorConfig) {
        this.sqlFilterUtils = sqlFilterUtils;
        this.dateModeUtils = dateModeUtils;
        this.executorConfig = executorConfig;
    }

    public static String getUnionSelect(QueryStructReq queryStructCmd) {
        StringBuilder sb = new StringBuilder();
        int locate = 0;
        for (String group : queryStructCmd.getGroups()) {
            if (group.contains(JOIN_UNDERLINE)) {
                group = group.split(JOIN_UNDERLINE)[1];
            }
            if (!TimeDimensionEnum.getNameList().contains(group)) {
                locate++;
                sb.append(group).append(" as ").append("name").append(locate).append(",");
            } else {
                sb.append(group).append(",");
            }
        }
        locate = 0;
        for (Aggregator agg : queryStructCmd.getAggregators()) {
            locate++;
            sb.append(agg.getColumn()).append(" as ").append("value").append(locate).append(",");
        }
        String selectSql = sb.substring(0, sb.length() - 1);
        log.info("union select sql {}", selectSql);
        return selectSql;
    }

    public String getLimit(QueryParam queryParam) {
        if (queryParam != null && queryParam.getLimit() != null && queryParam.getLimit() > 0) {
            return " limit " + queryParam.getLimit();
        }
        return "";
    }

    public String getSelect(QueryParam queryParam) {
        String aggStr = queryParam.getAggregators().stream().map(this::getSelectField)
                .collect(Collectors.joining(","));
        return CollectionUtils.isEmpty(queryParam.getGroups()) ? aggStr
                : String.join(",", queryParam.getGroups()) + "," + aggStr;
    }

    public String getSelect(QueryParam queryParam, Map<String, String> deriveMetrics) {
        String aggStr = queryParam.getAggregators().stream().map(a -> getSelectField(a, deriveMetrics))
                .collect(Collectors.joining(","));
        return CollectionUtils.isEmpty(queryParam.getGroups()) ? aggStr
                : String.join(",", queryParam.getGroups()) + "," + aggStr;
    }

    public String getSelectField(final Aggregator agg) {
        if (AggOperatorEnum.COUNT_DISTINCT.equals(agg.getFunc())) {
            return "count(distinct " + agg.getColumn() + " ) AS " + agg.getColumn() + " ";
        }
        if (CollectionUtils.isEmpty(agg.getArgs())) {
            return agg.getFunc() + "( " + agg.getColumn() + " ) AS " + agg.getColumn() + " ";
        }
        return agg.getFunc() + "( " + agg.getArgs().stream().map(arg ->
                arg.equals(agg.getColumn()) ? arg : (StringUtils.isNumeric(arg) ? arg : ("'" + arg + "'"))
        ).collect(Collectors.joining(",")) + " ) AS " + agg.getColumn() + " ";
    }

    public String getSelectField(final Aggregator agg, Map<String, String> deriveMetrics) {
        if (!deriveMetrics.containsKey(agg.getColumn())) {
            return getSelectField(agg);
        }
        return deriveMetrics.get(agg.getColumn());
    }

    public String getGroupBy(QueryParam queryParam) {
        if (CollectionUtils.isEmpty(queryParam.getGroups())) {
            return "";
        }
        return "group by " + String.join(",", queryParam.getGroups());
    }

    public String getOrderBy(QueryParam queryParam) {
        if (CollectionUtils.isEmpty(queryParam.getOrders())) {
            return "";
        }
        return "order by " + queryParam.getOrders().stream()
                .map(order -> " " + order.getColumn() + " " + order.getDirection() + " ")
                .collect(Collectors.joining(","));
    }

    public String getOrderBy(QueryParam queryParam, Map<String, String> deriveMetrics) {
        if (CollectionUtils.isEmpty(queryParam.getOrders())) {
            return "";
        }
        if (!queryParam.getOrders().stream().anyMatch(o -> deriveMetrics.containsKey(o.getColumn()))) {
            return getOrderBy(queryParam);
        }
        return "order by " + queryParam.getOrders().stream()
                .map(order -> " " + (deriveMetrics.containsKey(order.getColumn()) ? deriveMetrics.get(order.getColumn())
                        : order.getColumn()) + " " + order.getDirection() + " ")
                .collect(Collectors.joining(","));
    }

    public String generateWhere(QueryParam queryParam, ItemDateResp itemDateResp) {
        String whereClauseFromFilter = sqlFilterUtils.getWhereClause(queryParam.getDimensionFilters());
        String whereFromDate = getDateWhereClause(queryParam.getDateInfo(), itemDateResp);
        return mergeDateWhereClause(queryParam, whereClauseFromFilter, whereFromDate);
    }

    private String mergeDateWhereClause(QueryParam queryParam, String whereClauseFromFilter,
            String whereFromDate) {
        if (Strings.isNotEmpty(whereFromDate) && Strings.isNotEmpty(whereClauseFromFilter)) {
            return String.format("%s AND (%s)", whereFromDate, whereClauseFromFilter);
        } else if (Strings.isEmpty(whereFromDate) && Strings.isNotEmpty(whereClauseFromFilter)) {
            return whereClauseFromFilter;
        } else if (Strings.isNotEmpty(whereFromDate) && Strings.isEmpty(whereClauseFromFilter)) {
            return whereFromDate;
        } else if (Objects.isNull(whereFromDate) && Strings.isEmpty(whereClauseFromFilter)) {
            log.info("the current date information is empty, enter the date initialization logic");
            return dateModeUtils.defaultRecentDateInfo(queryParam.getDateInfo());
        }
        return whereClauseFromFilter;
    }

    public String getDateWhereClause(DateConf dateInfo, ItemDateResp dateDate) {
        if (Objects.isNull(dateDate)
                || Strings.isEmpty(dateDate.getStartDate())
                && Strings.isEmpty(dateDate.getEndDate())) {
            if (dateInfo.getDateMode().equals(DateConf.DateMode.LIST)) {
                return dateModeUtils.listDateStr(dateDate, dateInfo);
            }
            if (dateInfo.getDateMode().equals(DateConf.DateMode.BETWEEN)) {
                return dateModeUtils.betweenDateStr(dateDate, dateInfo);
            }
            if (dateModeUtils.hasAvailableDataMode(dateInfo)) {
                return dateModeUtils.hasDataModeStr(dateDate, dateInfo);
            }

            return dateModeUtils.defaultRecentDateInfo(dateInfo);
        }
        log.info("dateDate:{}", dateDate);
        return dateModeUtils.getDateWhereStr(dateInfo, dateDate);
    }

    public Triple<String, String, String> getBeginEndTime(QueryParam queryParam, ItemDateResp dataDate) {
        if (Objects.isNull(queryParam.getDateInfo())) {
            return Triple.of("", "", "");
        }
        DateConf dateConf = queryParam.getDateInfo();
        String dateInfo = dateModeUtils.getSysDateCol(dateConf);
        if (dateInfo.isEmpty()) {
            return Triple.of("", "", "");
        }
        switch (dateConf.getDateMode()) {
            case AVAILABLE:
            case BETWEEN:
                return Triple.of(dateInfo, dateConf.getStartDate(), dateConf.getEndDate());
            case LIST:
                return Triple.of(dateInfo, Collections.min(dateConf.getDateList()),
                        Collections.max(dateConf.getDateList()));
            case RECENT:
                LocalDate dateMax = LocalDate.now().minusDays(1);
                LocalDate dateMin = dateMax.minusDays(dateConf.getUnit() - 1);
                if (Objects.isNull(dataDate)) {
                    return Triple.of(dateInfo, dateMin.format(DateTimeFormatter.ofPattern(DAY_FORMAT)),
                            dateMax.format(DateTimeFormatter.ofPattern(DAY_FORMAT)));
                }
                switch (dateConf.getPeriod()) {
                    case DAY:
                        ImmutablePair<String, String> dayInfo = dateModeUtils.recentDay(dataDate, dateConf);
                        return Triple.of(dateInfo, dayInfo.left, dayInfo.right);
                    case WEEK:
                        ImmutablePair<String, String> weekInfo = dateModeUtils.recentWeek(dataDate, dateConf);
                        return Triple.of(dateInfo, weekInfo.left, weekInfo.right);
                    case MONTH:
                        List<ImmutablePair<String, String>> rets = dateModeUtils.recentMonth(dataDate, dateConf);
                        Optional<String> minBegins = rets.stream().map(i -> i.left).sorted().findFirst();
                        Optional<String> maxBegins = rets.stream().map(i -> i.right).sorted(Comparator.reverseOrder())
                                .findFirst();
                        if (minBegins.isPresent() && maxBegins.isPresent()) {
                            return Triple.of(dateInfo, minBegins.get(), maxBegins.get());
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;

        }
        return Triple.of("", "", "");
    }

    public boolean isSupportWith(EngineType engineTypeEnum, String version) {
        if (engineTypeEnum.equals(EngineType.MYSQL) && Objects.nonNull(version) && version.startsWith(
                executorConfig.getMysqlLowVersion())) {
            return false;
        }
        if (engineTypeEnum.equals(EngineType.CLICKHOUSE) && Objects.nonNull(version)
                && StringUtil.compareVersion(version,
                executorConfig.getCkLowVersion()) < 0) {
            return false;
        }
        return true;
    }

    public String generateInternalMetricName(String modelBizName) {
        return modelBizName + UNDERLINE + executorConfig.getInternalMetricNameSuffix();
    }

    public String generateDerivedMetric(final List<MetricSchemaResp> metricResps, final Set<String> allFields,
            final Map<String, Measure> allMeasures,
            final List<DimSchemaResp> dimensionResps,
            final String expression, final MetricDefineType metricDefineType,
            AggOption aggOption,
            Set<String> visitedMetric,
            Set<String> measures,
            Set<String> dimensions) {
        Set<String> fields = SqlSelectHelper.getColumnFromExpr(expression);
        if (!CollectionUtils.isEmpty(fields)) {
            Map<String, String> replace = new HashMap<>();
            for (String field : fields) {
                switch (metricDefineType) {
                    case METRIC:
                        Optional<MetricSchemaResp> metricItem = metricResps.stream()
                                .filter(m -> m.getBizName().equalsIgnoreCase(field)).findFirst();
                        if (metricItem.isPresent()) {
                            if (visitedMetric.contains(field)) {
                                break;
                            }
                            replace.put(field,
                                    generateDerivedMetric(metricResps, allFields, allMeasures, dimensionResps,
                                            getExpr(metricItem.get()), metricItem.get().getMetricDefineType(),
                                            aggOption, visitedMetric, measures, dimensions));
                            visitedMetric.add(field);
                        }
                        break;
                    case MEASURE:
                        if (allMeasures.containsKey(field)) {
                            measures.add(field);
                            replace.put(field, getExpr(allMeasures.get(field), aggOption));
                        }
                        break;
                    case FIELD:
                        if (allFields.contains(field)) {
                            Optional<DimSchemaResp> dimensionItem = dimensionResps.stream()
                                    .filter(d -> d.getBizName().equals(field)).findFirst();
                            if (dimensionItem.isPresent()) {
                                dimensions.add(field);
                            } else {
                                measures.add(field);
                            }
                        }
                        break;
                    default:
                        break;

                }
            }
            if (!CollectionUtils.isEmpty(replace)) {
                String expr = SqlReplaceHelper.replaceExpression(expression, replace);
                log.info("derived measure {}->{}", expression, expr);
                return expr;
            }
        }
        return expression;
    }

    public String getExpr(Measure measure, AggOption aggOption) {
        if (AggOperatorEnum.COUNT_DISTINCT.getOperator().equalsIgnoreCase(measure.getAgg())) {
            return aggOption.equals(AggOption.NATIVE) ? measure.getBizName()
                    : AggOperatorEnum.COUNT.getOperator() + " ( " + AggOperatorEnum.DISTINCT + " "
                            + measure.getBizName()
                            + " ) ";
        }
        return aggOption.equals(AggOption.NATIVE) ? measure.getBizName()
                : measure.getAgg() + " ( " + measure.getBizName() + " ) ";
    }

    public String getExpr(MetricResp metricResp) {
        if (Objects.isNull(metricResp.getMetricDefineType())) {
            return metricResp.getMetricDefineByMeasureParams().getExpr();
        }
        if (metricResp.getMetricDefineType().equals(MetricDefineType.METRIC)) {
            return metricResp.getMetricDefineByMetricParams().getExpr();
        }
        if (metricResp.getMetricDefineType().equals(MetricDefineType.FIELD)) {
            return metricResp.getMetricDefineByFieldParams().getExpr();
        }
        // measure add agg function
        return metricResp.getMetricDefineByMeasureParams().getExpr();
    }
}
