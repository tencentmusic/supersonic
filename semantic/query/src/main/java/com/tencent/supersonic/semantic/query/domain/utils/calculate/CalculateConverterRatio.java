package com.tencent.supersonic.semantic.query.domain.utils.calculate;

import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;
import com.tencent.supersonic.semantic.api.query.enums.FilterOperatorEnum;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.pojo.MetricTable;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.semantic.core.domain.DomainService;
import com.tencent.supersonic.semantic.query.domain.utils.QueryStructUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 *
 */
@Slf4j
@Component
public class CalculateConverterRatio implements CalculateConverter {

    private final DomainService domainService;
    private final QueryStructUtils queryStructUtils;

    @Value("${metricParser.agg.ratio_roll.name:ratio_roll}")
    private String metricAggRatioRollName;

    @Value("${metricParser.agg.ratio_over.week:ratio_over_week}") // week over
    private String metricAggRatioOverWeek;
    @Value("${metricParser.agg.ratio_over.month:ratio_over_month}")
    private String metricAggRatioOverMonth;
    @Value("${metricParser.agg.ratio_over.quarter:ratio_over_quarter}")
    private String metricAggRatioOverQuarter;
    @Value("${metricParser.agg.ratio_over.year:ratio_over_year}")
    private String metricAggRatioOverYear;
    private List<String> dateGrain = new ArrayList<>(Arrays.asList("day", "week", "month", "year", "quarter"));
    private Set<String> aggFunctionsOver = new HashSet<>(
            Arrays.asList(metricAggRatioOverWeek, metricAggRatioOverMonth, metricAggRatioOverQuarter,
                    metricAggRatioOverYear));

    public CalculateConverterRatio(DomainService domainService,
            @Lazy QueryStructUtils queryStructUtils) {
        this.domainService = domainService;
        this.queryStructUtils = queryStructUtils;
    }


    public boolean accept(QueryStructReq queryStructCmd) {
        Long ratioFuncNum = queryStructCmd.getAggregators().stream()
                .filter(f -> f.getArgs() != null && f.getArgs().get(0) != null
                        && metricAggRatioRollName.equalsIgnoreCase(f.getArgs().get(0))).count();
        if (ratioFuncNum > 0) {
            return true;
        }
        return false;
    }


    public SqlParserResp getSqlParser(QueryStructReq queryStructCmd) throws Exception {
        return null;
    }


    /**
     * @param queryStructCmd
     * @return
     */
    public ParseSqlReq generateSqlCommand(QueryStructReq queryStructCmd) throws Exception {
        check(queryStructCmd);
        ParseSqlReq sqlCommand = new ParseSqlReq();
        sqlCommand.setRootPath(domainService.getDomainFullPath(queryStructCmd.getDomainId()));
        String metricTableName = "metric_tb";
        MetricTable metricTable = new MetricTable();
        metricTable.setAlias(metricTableName);
        metricTable.setMetrics(queryStructCmd.getMetrics());
        metricTable.setDimensions(queryStructCmd.getGroups());
        String where = queryStructUtils.generateWhere(queryStructCmd);
        log.info("in generateSqlCommend, complete where:{}", where);
//        metricTable.setWhere(queryStructCmd.getWhereClause());
        metricTable.setWhere(where);
        metricTable.setAgg(false);
        sqlCommand.setTables(new ArrayList<>(Collections.singletonList(metricTable)));
        String sqlInner = String.format("select %s from %s  %s  ", getSelect(queryStructCmd), metricTableName,
                getGroupBy(queryStructCmd));
        String sql = String.format(
                "select %s from ( select %s , %s from ( %s ) metric_tb_inner_1 ) metric_tb_src %s %s ",
                getOverSelect(queryStructCmd), getSelect(queryStructCmd, true), getLeadSelect(queryStructCmd), sqlInner,
                getOrderBy(queryStructCmd), getLimit(queryStructCmd));

        sqlCommand.setSql(sql);
        return sqlCommand;
    }

    private String getOverSelect(QueryStructReq queryStructCmd) {
        String timeDim = getTimeDim(queryStructCmd);
        String timeSpan = "INTERVAL  " + getTimeSpan(queryStructCmd);
        String aggStr = queryStructCmd.getAggregators().stream().map(f -> {
            if (f.getArgs() != null && f.getArgs().size() > 0) {
                return String.format("if(%s = date_add(%s_roll,%s) and %s_roll!=0,  (%s-%s_roll)/%s_roll , 0) as %s",
                        timeDim, timeDim, timeSpan, f.getColumn(), f.getColumn(), f.getColumn(), f.getColumn(),
                        f.getColumn());
            } else {
                return f.getColumn();
            }
        }).collect(Collectors.joining(","));
        return CollectionUtils.isEmpty(queryStructCmd.getGroups()) ? aggStr
                : String.join(",", queryStructCmd.getGroups()) + "," + aggStr;
    }

    private String getLeadSelect(QueryStructReq queryStructCmd) {
        String timeDim = getTimeDim(queryStructCmd);
        String groupDimWithOutTime = getGroupDimWithOutTime(queryStructCmd);
        String aggStr = queryStructCmd.getAggregators().stream().map(f -> {
            if (f.getArgs() != null && f.getArgs().size() > 0 && f.getArgs().get(0)
                    .equalsIgnoreCase(metricAggRatioRollName)) {
                return String.format("lead(%s ,1,0) over ( %s order by %s desc) as %s_roll", f.getColumn(),
                        !groupDimWithOutTime.isEmpty() ? " partition by " + groupDimWithOutTime : "", timeDim,
                        f.getColumn());
            } else {
                return "";
            }
        }).filter(f -> !f.isEmpty()).collect(Collectors.joining(","));
        String timeDimLead = String.format("lead(cast(%s as string) ,1,'') over ( %s order by %s desc) as %s_roll",
                timeDim, !groupDimWithOutTime.isEmpty() ? " partition by " + groupDimWithOutTime : "", timeDim,
                timeDim);
        return timeDimLead + " , " + aggStr;
    }

    private String getTimeSpan(QueryStructReq queryStructCmd) {
        String timeGrain = getTimeDimGrain(queryStructCmd).toLowerCase();
        if ("week".equalsIgnoreCase(timeGrain)) {
            return "7 day";
        }
        if ("quarter".equalsIgnoreCase(timeGrain)) {
            return "3 month";
        }
        return "1 " + timeGrain;
    }

    private String getTimeDimGrain(QueryStructReq queryStructCmd) {
        String grain = queryStructCmd.getAggregators().stream().map(f -> {
            if (f.getArgs() != null && f.getArgs().size() > 1 && f.getArgs().get(0)
                    .equalsIgnoreCase(metricAggRatioRollName)) {
                return f.getArgs().get(1);
            }
            return "";
        }).filter(f -> !f.isEmpty()).findFirst().orElse("");
        return grain.isEmpty() ? "day" : grain;
    }

    private String getGroupDimWithOutTime(QueryStructReq queryStructCmd) {
        String timeDim = getTimeDim(queryStructCmd);
        return queryStructCmd.getGroups().stream().filter(f -> !f.equalsIgnoreCase(timeDim))
                .collect(Collectors.joining(","));
    }

    private String getTimeDim(QueryStructReq queryStructCmd) {
        String dsField = "";
        String dsStart = "";
        String dsEnd = "";

        for (Filter filter : queryStructCmd.getOriginalFilter()) {
            if (Filter.Relation.FILTER.equals(filter.getRelation())) {
                // TODO get parameters from DateInfo
                if ("DATE".equalsIgnoreCase(filter.getRelation().name())) {
                    if (FilterOperatorEnum.GREATER_THAN_EQUALS.getValue()
                            .equalsIgnoreCase(filter.getOperator().toString())
                            || FilterOperatorEnum.GREATER_THAN.getValue()
                            .equalsIgnoreCase(filter.getOperator().toString())) {
                        dsField = filter.getBizName();
                        dsStart = filter.getValue().toString();
                    }
                    if (FilterOperatorEnum.MINOR_THAN_EQUALS.getValue()
                            .equalsIgnoreCase(filter.getOperator().toString())
                            || FilterOperatorEnum.MINOR_THAN.getValue()
                            .equalsIgnoreCase(filter.getOperator().toString())) {
                        dsField = filter.getBizName();
                        dsEnd = filter.getValue().toString();
                    }
                }

            }
        }
        return dsField;
    }

    private String getLimit(QueryStructReq queryStructCmd) {
        if (queryStructCmd.getLimit() > 0) {
            return " limit " + String.valueOf(queryStructCmd.getLimit());
        }
        return "";
    }

    private String getSelect(QueryStructReq queryStructCmd) {
        return getSelect(queryStructCmd, false);
    }

    private String getSelect(QueryStructReq queryStructCmd, boolean isRatio) {
        String aggStr = queryStructCmd.getAggregators().stream().map(f -> getSelectField(f, isRatio))
                .collect(Collectors.joining(","));
        return CollectionUtils.isEmpty(queryStructCmd.getGroups()) ? aggStr
                : String.join(",", queryStructCmd.getGroups()) + "," + aggStr;
    }

    private String getSelectField(final Aggregator agg, boolean isRatio) {
        if (!CollectionUtils.isEmpty(agg.getArgs()) && agg.getArgs().size() > 0) {
            if (agg.getArgs().get(0).equalsIgnoreCase(metricAggRatioRollName)) {
                if (isRatio) {
                    return agg.getColumn();
                }
                return agg.getFunc().name().isEmpty() ? agg.getColumn()
                        : agg.getFunc() + "( " + agg.getColumn() + " ) AS " + agg.getColumn() + " ";
            }
        }
        if (CollectionUtils.isEmpty(agg.getArgs())) {
            return agg.getFunc() + "( " + agg.getColumn() + " ) AS " + agg.getColumn() + " ";
        }
        return agg.getFunc() + "( " + agg.getArgs().stream().map(arg ->
                arg.equals(agg.getColumn()) ? arg : (StringUtils.isNumeric(arg) ? arg : ("'" + arg + "'"))
        ).collect(Collectors.joining(",")) + " ) AS " + agg.getColumn() + " ";
    }

    private String getGroupBy(QueryStructReq queryStructCmd) {
        if (CollectionUtils.isEmpty(queryStructCmd.getGroups())) {
            return "";
        }
        return "group by " + String.join(",", queryStructCmd.getGroups());
    }

    private String getOrderBy(QueryStructReq queryStructCmd) {
        if (CollectionUtils.isEmpty(queryStructCmd.getOrders())) {
            return "";
        }
        return "order by " + queryStructCmd.getOrders().stream()
                .map(order -> " " + order.getColumn() + " " + order.getDirection() + " ")
                .collect(Collectors.joining(","));
    }

    private void check(QueryStructReq queryStructCmd) throws Exception {
        Set<String> aggFunctions = queryStructCmd.getAggregators().stream()
                .filter(f -> f.getArgs() != null && f.getArgs().get(0) != null)
                .map(agg -> agg.getArgs().get(0).toLowerCase()).collect(Collectors.toSet());
        Long ratioOverNum = aggFunctions.stream().filter(aggFunctionsOver::contains).count();
        if (ratioOverNum > 0) {
            throw new Exception("not support over ratio");
        }
        if (aggFunctions.contains(metricAggRatioRollName)) {
            if (ratioOverNum > 0) {
                throw new Exception("not support over ratio and roll ratio together ");
            }
        }
        if (getTimeDim(queryStructCmd).isEmpty()) {
            throw new Exception("miss time filter");
        }
        String timeDimGrain = getTimeDimGrain(queryStructCmd).toLowerCase();
        if (!dateGrain.contains(timeDimGrain)) {
            throw new Exception("second arg must be [day week month year quarter] ");
        }
    }
}
