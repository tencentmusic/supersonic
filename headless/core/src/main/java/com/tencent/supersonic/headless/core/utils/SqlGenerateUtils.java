package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.common.util.DateModeUtils;
import com.tencent.supersonic.common.util.SqlFilterUtils;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.core.config.ExecutorConfig;
import com.tencent.supersonic.headless.core.pojo.StructQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.tencent.supersonic.common.pojo.Constants.DAY_FORMAT;
import static com.tencent.supersonic.common.pojo.Constants.JOIN_UNDERLINE;

/** tools functions to analyze queryStructReq */
@Component
@Slf4j
public class SqlGenerateUtils {

    private final SqlFilterUtils sqlFilterUtils;

    private final DateModeUtils dateModeUtils;

    private final ExecutorConfig executorConfig;

    public SqlGenerateUtils(SqlFilterUtils sqlFilterUtils, DateModeUtils dateModeUtils,
            ExecutorConfig executorConfig) {
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
            sb.append(group).append(",");
        }
        locate = 0;
        for (Aggregator agg : queryStructCmd.getAggregators()) {
            locate++;
            sb.append(agg.getColumn()).append(" as ").append("value").append(locate).append(",");
        }
        String selectSql = sb.substring(0, sb.length() - 1);
        log.debug("union select sql {}", selectSql);
        return selectSql;
    }

    public String getLimit(StructQuery structQuery) {
        if (structQuery != null && structQuery.getLimit() != null && structQuery.getLimit() > 0) {
            return " limit " + structQuery.getLimit();
        }
        return "";
    }

    public String getSelect(StructQuery structQuery) {
        String aggStr = structQuery.getAggregators().stream().map(this::getSelectField)
                .collect(Collectors.joining(","));
        String result = String.join(",", structQuery.getGroups());
        if (StringUtils.isNotBlank(aggStr)) {
            if (!CollectionUtils.isEmpty(structQuery.getGroups())) {
                result = String.join(",", structQuery.getGroups()) + "," + aggStr;
            } else {
                result = aggStr;
            }
        }

        return result;
    }

    public String getSelect(StructQuery structQuery, Map<String, String> deriveMetrics) {
        String aggStr = structQuery.getAggregators().stream()
                .map(a -> getSelectField(a, deriveMetrics)).collect(Collectors.joining(","));
        String result = String.join(",", structQuery.getGroups());
        if (StringUtils.isNotBlank(aggStr)) {
            if (!CollectionUtils.isEmpty(structQuery.getGroups())) {
                result = String.join(",", structQuery.getGroups()) + "," + aggStr;
            } else {
                result = aggStr;
            }
        }

        return result;
    }

    public String getSelectField(final Aggregator agg) {
        if (AggOperatorEnum.COUNT_DISTINCT.equals(agg.getFunc())) {
            return "count(distinct " + agg.getColumn() + " ) ";
        }
        if (CollectionUtils.isEmpty(agg.getArgs())) {
            return agg.getFunc() + "( " + agg.getColumn() + " ) ";
        }
        return agg.getFunc() + "( "
                + agg.getArgs().stream()
                        .map(arg -> arg.equals(agg.getColumn()) ? arg
                                : (StringUtils.isNumeric(arg) ? arg : ("'" + arg + "'")))
                        .collect(Collectors.joining(","))
                + " ) ";
    }

    public String getSelectField(final Aggregator agg, Map<String, String> deriveMetrics) {
        if (!deriveMetrics.containsKey(agg.getColumn())) {
            return getSelectField(agg);
        }
        return deriveMetrics.get(agg.getColumn());
    }

    public String getGroupBy(StructQuery structQuery) {
        if (CollectionUtils.isEmpty(structQuery.getGroups())) {
            return "";
        }
        return "group by " + String.join(",", structQuery.getGroups());
    }

    public String getOrderBy(StructQuery structQuery) {
        if (CollectionUtils.isEmpty(structQuery.getOrders())) {
            return "";
        }
        return "order by " + structQuery.getOrders().stream()
                .map(order -> " " + order.getColumn() + " " + order.getDirection() + " ")
                .collect(Collectors.joining(","));
    }

    public String getOrderBy(StructQuery structQuery, Map<String, String> deriveMetrics) {
        if (CollectionUtils.isEmpty(structQuery.getOrders())) {
            return "";
        }
        if (!structQuery.getOrders().stream()
                .anyMatch(o -> deriveMetrics.containsKey(o.getColumn()))) {
            return getOrderBy(structQuery);
        }
        return "order by " + structQuery.getOrders().stream()
                .map(order -> " " + (deriveMetrics.containsKey(order.getColumn())
                        ? deriveMetrics.get(order.getColumn())
                        : order.getColumn()) + " " + order.getDirection() + " ")
                .collect(Collectors.joining(","));
    }

    public String generateWhere(StructQuery structQuery, ItemDateResp itemDateResp) {
        String whereClauseFromFilter =
                sqlFilterUtils.getWhereClause(structQuery.getDimensionFilters());
        String whereFromDate = "";
        if (structQuery.getDateInfo() != null) {
            whereFromDate = getDateWhereClause(structQuery.getDateInfo(), itemDateResp);
        }
        String mergedWhere =
                mergeDateWhereClause(structQuery, whereClauseFromFilter, whereFromDate);
        if (StringUtils.isNotBlank(mergedWhere)) {
            mergedWhere = "where " + mergedWhere;
        }
        return mergedWhere;
    }

    private String mergeDateWhereClause(StructQuery structQuery, String whereClauseFromFilter,
            String whereFromDate) {
        if (StringUtils.isNotEmpty(whereFromDate)
                && StringUtils.isNotEmpty(whereClauseFromFilter)) {
            return String.format("%s AND (%s)", whereFromDate, whereClauseFromFilter);
        } else if (StringUtils.isEmpty(whereFromDate)
                && StringUtils.isNotEmpty(whereClauseFromFilter)) {
            return whereClauseFromFilter;
        } else if (StringUtils.isNotEmpty(whereFromDate)
                && StringUtils.isEmpty(whereClauseFromFilter)) {
            return whereFromDate;
        } else if (Objects.isNull(whereFromDate) && StringUtils.isEmpty(whereClauseFromFilter)) {
            log.debug("the current date information is empty, enter the date initialization logic");
            return dateModeUtils.defaultRecentDateInfo(structQuery.getDateInfo());
        }
        return whereClauseFromFilter;
    }

    public String getDateWhereClause(DateConf dateInfo, ItemDateResp dateDate) {
        if (Objects.isNull(dateDate) || StringUtils.isEmpty(dateDate.getStartDate())
                && StringUtils.isEmpty(dateDate.getEndDate())) {
            if (dateInfo.getDateMode().equals(DateConf.DateMode.LIST)) {
                return dateModeUtils.listDateStr(dateInfo);
            }
            if (dateInfo.getDateMode().equals(DateConf.DateMode.BETWEEN)) {
                return dateModeUtils.betweenDateStr(dateInfo);
            }
            if (dateModeUtils.hasAvailableDataMode(dateInfo)) {
                return dateModeUtils.hasDataModeStr(dateDate, dateInfo);
            }

            return dateModeUtils.defaultRecentDateInfo(dateInfo);
        }
        log.debug("dateDate:{}", dateDate);
        return dateModeUtils.getDateWhereStr(dateInfo, dateDate);
    }

    public Triple<String, String, String> getBeginEndTime(StructQuery structQuery,
            ItemDateResp dataDate) {
        if (Objects.isNull(structQuery.getDateInfo())) {
            return Triple.of("", "", "");
        }
        DateConf dateConf = structQuery.getDateInfo();
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
                    return Triple.of(dateInfo,
                            dateMin.format(DateTimeFormatter.ofPattern(DAY_FORMAT)),
                            dateMax.format(DateTimeFormatter.ofPattern(DAY_FORMAT)));
                }
                switch (dateConf.getPeriod()) {
                    case DAY:
                        ImmutablePair<String, String> dayInfo =
                                dateModeUtils.recentDay(dataDate, dateConf);
                        return Triple.of(dateInfo, dayInfo.left, dayInfo.right);
                    case WEEK:
                        ImmutablePair<String, String> weekInfo =
                                dateModeUtils.recentWeek(dataDate, dateConf);
                        return Triple.of(dateInfo, weekInfo.left, weekInfo.right);
                    case MONTH:
                        List<ImmutablePair<String, String>> rets =
                                dateModeUtils.recentMonth(dataDate, dateConf);
                        Optional<String> minBegins =
                                rets.stream().map(i -> i.left).sorted().findFirst();
                        Optional<String> maxBegins = rets.stream().map(i -> i.right)
                                .sorted(Comparator.reverseOrder()).findFirst();
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
        if (engineTypeEnum.equals(EngineType.MYSQL) && Objects.nonNull(version)
                && StringUtil.compareVersion(version, executorConfig.getMysqlLowVersion()) < 0) {
            return false;
        }
        if (engineTypeEnum.equals(EngineType.CLICKHOUSE) && Objects.nonNull(version)
                && StringUtil.compareVersion(version, executorConfig.getCkLowVersion()) < 0) {
            return false;
        }
        return true;
    }

}
