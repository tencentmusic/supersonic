package com.tencent.supersonic.headless.core.utils;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.ItemDateResp;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.DateModeUtils;
import com.tencent.supersonic.common.util.SqlFilterUtils;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.headless.api.enums.EngineType;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.tencent.supersonic.common.pojo.Constants.DAY;
import static com.tencent.supersonic.common.pojo.Constants.DAY_FORMAT;
import static com.tencent.supersonic.common.pojo.Constants.JOIN_UNDERLINE;
import static com.tencent.supersonic.common.pojo.Constants.MONTH;
import static com.tencent.supersonic.common.pojo.Constants.UNDERLINE;
import static com.tencent.supersonic.common.pojo.Constants.WEEK;

@Component
@Slf4j
public class SqlGenerateUtils {

    private final SqlFilterUtils sqlFilterUtils;

    private final DateModeUtils dateModeUtils;

    @Value("${metricParser.agg.mysql.lowVersion:5.7}")
    private String mysqlLowVersion;
    @Value("${metricParser.agg.ck.lowVersion:20.4}")
    private String ckLowVersion;
    @Value("${internal.metric.cnt.suffix:internal_cnt}")
    private String internalMetricNameSuffix;

    public SqlGenerateUtils(SqlFilterUtils sqlFilterUtils,
                            DateModeUtils dateModeUtils) {
        this.sqlFilterUtils = sqlFilterUtils;
        this.dateModeUtils = dateModeUtils;
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

    public String getLimit(QueryStructReq queryStructCmd) {
        if (queryStructCmd.getLimit() > 0) {
            return " limit " + queryStructCmd.getLimit();
        }
        return "";
    }

    public String getSelect(QueryStructReq queryStructCmd) {
        String aggStr = queryStructCmd.getAggregators().stream().map(this::getSelectField)
                .collect(Collectors.joining(","));
        return CollectionUtils.isEmpty(queryStructCmd.getGroups()) ? aggStr
                : String.join(",", queryStructCmd.getGroups()) + "," + aggStr;
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

    public String getGroupBy(QueryStructReq queryStructCmd) {
        if (CollectionUtils.isEmpty(queryStructCmd.getGroups())) {
            return "";
        }
        return "group by " + String.join(",", queryStructCmd.getGroups());
    }

    public String getOrderBy(QueryStructReq queryStructCmd) {
        if (CollectionUtils.isEmpty(queryStructCmd.getOrders())) {
            return "";
        }
        return "order by " + queryStructCmd.getOrders().stream()
                .map(order -> " " + order.getColumn() + " " + order.getDirection() + " ")
                .collect(Collectors.joining(","));
    }

    public String generateWhere(QueryStructReq queryStructReq, ItemDateResp itemDateResp) {
        String whereClauseFromFilter = sqlFilterUtils.getWhereClause(queryStructReq.getOriginalFilter());
        String whereFromDate = getDateWhereClause(queryStructReq.getDateInfo(), itemDateResp);
        return mergeDateWhereClause(queryStructReq, whereClauseFromFilter, whereFromDate);
    }

    private String mergeDateWhereClause(QueryStructReq queryStructCmd, String whereClauseFromFilter,
                                       String whereFromDate) {
        if (Strings.isNotEmpty(whereFromDate) && Strings.isNotEmpty(whereClauseFromFilter)) {
            return String.format("%s AND (%s)", whereFromDate, whereClauseFromFilter);
        } else if (Strings.isEmpty(whereFromDate) && Strings.isNotEmpty(whereClauseFromFilter)) {
            return whereClauseFromFilter;
        } else if (Strings.isNotEmpty(whereFromDate) && Strings.isEmpty(whereClauseFromFilter)) {
            return whereFromDate;
        } else if (Objects.isNull(whereFromDate) && Strings.isEmpty(whereClauseFromFilter)) {
            log.info("the current date information is empty, enter the date initialization logic");
            return dateModeUtils.defaultRecentDateInfo(queryStructCmd.getDateInfo());
        }
        return whereClauseFromFilter;
    }

    private String getDateWhereClause(DateConf dateInfo, ItemDateResp dateDate) {
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

    public Triple<String, String, String> getBeginEndTime(QueryStructReq queryStructCmd, ItemDateResp dataDate) {
        if (Objects.isNull(queryStructCmd.getDateInfo())) {
            return Triple.of("", "", "");
        }
        DateConf dateConf = queryStructCmd.getDateInfo();
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
                mysqlLowVersion)) {
            return false;
        }
        if (engineTypeEnum.equals(EngineType.CLICKHOUSE) && Objects.nonNull(version)
                && StringUtil.compareVersion(version,
                ckLowVersion) < 0) {
            return false;
        }
        return true;
    }

    public String generateInternalMetricName(String modelBizName) {
        return modelBizName + UNDERLINE + internalMetricNameSuffix;
    }

}
