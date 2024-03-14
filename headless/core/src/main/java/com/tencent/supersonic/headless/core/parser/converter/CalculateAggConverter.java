package com.tencent.supersonic.headless.core.parser.converter;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.DateModeUtils;
import com.tencent.supersonic.headless.api.pojo.MetricTable;
import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.enums.EngineType;
import com.tencent.supersonic.headless.core.pojo.DataSetQueryParam;
import com.tencent.supersonic.headless.core.pojo.Database;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * supplement the QueryStatement when query with custom aggregation method
 */
@Component("CalculateAggConverter")
@Slf4j
public class CalculateAggConverter implements HeadlessConverter {


    public interface EngineSql {

        String sql(QueryParam queryParam, boolean isOver, boolean asWith, String metricSql);
    }

    public DataSetQueryParam generateSqlCommend(QueryStatement queryStatement,
            EngineType engineTypeEnum, String version)
            throws Exception {
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        QueryParam queryParam = queryStatement.getQueryParam();
        // 同环比
        if (isRatioAccept(queryParam)) {
            return generateRatioSqlCommand(queryStatement, engineTypeEnum, version);
        }
        DataSetQueryParam sqlCommand = new DataSetQueryParam();
        String metricTableName = "v_metric_tb_tmp";
        MetricTable metricTable = new MetricTable();
        metricTable.setAlias(metricTableName);
        metricTable.setMetrics(queryParam.getMetrics());
        metricTable.setDimensions(queryParam.getGroups());
        String where = sqlGenerateUtils.generateWhere(queryParam, null);
        log.info("in generateSqlCommand, complete where:{}", where);
        metricTable.setWhere(where);
        metricTable.setAggOption(AggOption.AGGREGATION);
        sqlCommand.setTables(new ArrayList<>(Collections.singletonList(metricTable)));
        String sql = String.format("select %s from %s  %s %s %s", sqlGenerateUtils.getSelect(queryParam),
                metricTableName,
                sqlGenerateUtils.getGroupBy(queryParam), sqlGenerateUtils.getOrderBy(queryParam),
                sqlGenerateUtils.getLimit(queryParam));
        if (!sqlGenerateUtils.isSupportWith(engineTypeEnum, version)) {
            sqlCommand.setSupportWith(false);
            sql = String.format("select %s from %s t0 %s %s %s", sqlGenerateUtils.getSelect(queryParam),
                    metricTableName,
                    sqlGenerateUtils.getGroupBy(queryParam), sqlGenerateUtils.getOrderBy(queryParam),
                    sqlGenerateUtils.getLimit(queryParam));
        }
        sqlCommand.setSql(sql);
        return sqlCommand;
    }

    @Override
    public boolean accept(QueryStatement queryStatement) {
        if (Objects.isNull(queryStatement.getQueryParam()) || queryStatement.getIsS2SQL()) {
            return false;
        }
        QueryParam queryParam = queryStatement.getQueryParam();
        if (queryParam.getQueryType().isNativeAggQuery()) {
            return false;
        }
        if (CollectionUtils.isEmpty(queryParam.getAggregators())) {
            return false;
        }

        int nonSumFunction = 0;
        for (Aggregator agg : queryParam.getAggregators()) {
            if (agg.getFunc() == null || "".equals(agg.getFunc())) {
                return false;
            }
            if (agg.getFunc().equals(AggOperatorEnum.UNKNOWN)) {
                return false;
            }
            if (agg.getFunc() != null) {
                nonSumFunction++;
            }
        }
        return nonSumFunction > 0;
    }

    @Override
    public void convert(QueryStatement queryStatement) throws Exception {
        DataSetQueryParam sqlCommend = queryStatement.getDataSetQueryParam();
        Database database = queryStatement.getSemanticModel().getDatabase();
        DataSetQueryParam dataSetQueryParam = generateSqlCommend(queryStatement,
                EngineType.fromString(database.getType().toUpperCase()), database.getVersion());
        sqlCommend.setSql(dataSetQueryParam.getSql());
        sqlCommend.setTables(dataSetQueryParam.getTables());
        sqlCommend.setSupportWith(dataSetQueryParam.isSupportWith());
    }

    /**
     * Ratio
     */

    public boolean isRatioAccept(QueryParam queryParam) {
        Long ratioFuncNum = queryParam.getAggregators().stream()
                .filter(f -> (f.getFunc().equals(AggOperatorEnum.RATIO_ROLL) || f.getFunc()
                        .equals(AggOperatorEnum.RATIO_OVER))).count();
        if (ratioFuncNum > 0) {
            return true;
        }
        return false;
    }

    public DataSetQueryParam generateRatioSqlCommand(QueryStatement queryStatement, EngineType engineTypeEnum,
            String version)
            throws Exception {
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        QueryParam queryParam = queryStatement.getQueryParam();
        check(queryParam);
        queryStatement.setEnableOptimize(false);
        DataSetQueryParam sqlCommand = new DataSetQueryParam();
        String metricTableName = "v_metric_tb_tmp";
        MetricTable metricTable = new MetricTable();
        metricTable.setAlias(metricTableName);
        metricTable.setMetrics(queryParam.getMetrics());
        metricTable.setDimensions(queryParam.getGroups());
        String where = sqlGenerateUtils.generateWhere(queryParam, null);
        log.info("in generateSqlCommend, complete where:{}", where);
        metricTable.setWhere(where);
        metricTable.setAggOption(AggOption.AGGREGATION);
        sqlCommand.setTables(new ArrayList<>(Collections.singletonList(metricTable)));
        boolean isOver = isOverRatio(queryParam);
        String sql = "";
        switch (engineTypeEnum) {
            case H2:
                sql = new H2EngineSql().sql(queryParam, isOver, true, metricTableName);
                break;
            case MYSQL:
            case DORIS:
            case CLICKHOUSE:
                if (!sqlGenerateUtils.isSupportWith(engineTypeEnum, version)) {
                    sqlCommand.setSupportWith(false);
                }
                if (!engineTypeEnum.equals(engineTypeEnum.CLICKHOUSE)) {
                    sql = new MysqlEngineSql().sql(queryParam, isOver, sqlCommand.isSupportWith(), metricTableName);
                } else {
                    sql = new CkEngineSql().sql(queryParam, isOver, sqlCommand.isSupportWith(), metricTableName);
                }
                break;
            default:
        }
        sqlCommand.setSql(sql);
        return sqlCommand;
    }

    public class H2EngineSql implements EngineSql {

        public String getOverSelect(QueryParam queryParam, boolean isOver) {
            String aggStr = queryParam.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER) || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    return String.format("( (%s-%s_roll)/cast(%s_roll as DOUBLE) ) as %s_%s,%s",
                            f.getColumn(), f.getColumn(), f.getColumn(), f.getColumn(),
                            f.getFunc().getOperator(), f.getColumn());
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(","));
            return CollectionUtils.isEmpty(queryParam.getGroups()) ? aggStr
                    : String.join(",", queryParam.getGroups()) + "," + aggStr;
        }

        public String getTimeSpan(QueryParam queryParam, boolean isOver, boolean isAdd) {
            if (Objects.nonNull(queryParam.getDateInfo())) {
                String addStr = isAdd ? "" : "-";
                if (queryParam.getDateInfo().getPeriod().equalsIgnoreCase(Constants.DAY)) {
                    return "day," + (isOver ? addStr + "7" : addStr + "1");
                }
                if (queryParam.getDateInfo().getPeriod().equalsIgnoreCase(Constants.WEEK)) {
                    return isOver ? "month," + addStr + "1" : "day," + addStr + "7";
                }
                if (queryParam.getDateInfo().getPeriod().equalsIgnoreCase(Constants.MONTH)) {
                    return isOver ? "year," + addStr + "1" : "month," + addStr + "1";
                }
            }
            return "";
        }

        public String getJoinOn(QueryParam queryParam, boolean isOver, String aliasLeft, String aliasRight) {
            String timeDim = getTimeDim(queryParam);
            String timeSpan = getTimeSpan(queryParam, isOver, true);
            String aggStr = queryParam.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER) || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    if (queryParam.getDateInfo().getPeriod().equals(Constants.MONTH)) {
                        return String.format(
                                "%s is not null and %s = FORMATDATETIME(DATEADD(%s,CONCAT(%s,'-01')),'yyyy-MM') ",
                                aliasRight + timeDim, aliasLeft + timeDim, timeSpan, aliasRight + timeDim);
                    }
                    if (queryParam.getDateInfo().getPeriod().equals(Constants.WEEK) && isOver) {
                        return String.format(" DATE_TRUNC('week',DATEADD(%s,%s) ) = %s ",
                                getTimeSpan(queryParam, isOver, false), aliasLeft + timeDim, aliasRight + timeDim);
                    }
                    return String.format("%s = TIMESTAMPADD(%s,%s) ",
                            aliasLeft + timeDim, timeSpan, aliasRight + timeDim);
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(" and "));
            List<String> groups = new ArrayList<>();
            for (String group : queryParam.getGroups()) {
                if (group.equalsIgnoreCase(timeDim)) {
                    continue;
                }
                groups.add(aliasLeft + group + " = " + aliasRight + group);
            }
            return CollectionUtils.isEmpty(groups) ? aggStr
                    : String.join(" and ", groups) + " and " + aggStr + " ";
        }

        @Override
        public String sql(QueryParam queryParam, boolean isOver, boolean asWith, String metricSql) {
            String sql = String.format(
                    "select %s from ( select %s , %s from %s t0 left join %s t1 on %s ) metric_tb_src %s %s ",
                    getOverSelect(queryParam, isOver), getAllSelect(queryParam, "t0."),
                    getAllJoinSelect(queryParam, "t1."), metricSql, metricSql,
                    getJoinOn(queryParam, isOver, "t0.", "t1."),
                    getOrderBy(queryParam), getLimit(queryParam));
            return sql;
        }
    }

    public class CkEngineSql extends MysqlEngineSql {

        public String getJoinOn(QueryParam queryParam, boolean isOver, String aliasLeft, String aliasRight) {
            String timeDim = getTimeDim(queryParam);
            String timeSpan = "INTERVAL  " + getTimeSpan(queryParam, isOver, true);
            String aggStr = queryParam.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER) || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    if (queryParam.getDateInfo().getPeriod().equals(Constants.MONTH)) {
                        return String.format("toDate(CONCAT(%s,'-01')) = date_add(toDate(CONCAT(%s,'-01')),%s)  ",
                                aliasLeft + timeDim, aliasRight + timeDim, timeSpan);
                    }
                    if (queryParam.getDateInfo().getPeriod().equals(Constants.WEEK) && isOver) {
                        return String.format("toMonday(date_add(%s ,INTERVAL %s) ) = %s",
                                aliasLeft + timeDim, getTimeSpan(queryParam, isOver, false), aliasRight + timeDim);
                    }
                    return String.format("%s = date_add(%s,%s) ",
                            aliasLeft + timeDim, aliasRight + timeDim, timeSpan);
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(" and "));
            List<String> groups = new ArrayList<>();
            for (String group : queryParam.getGroups()) {
                if (group.equalsIgnoreCase(timeDim)) {
                    continue;
                }
                groups.add(aliasLeft + group + " = " + aliasRight + group);
            }
            return CollectionUtils.isEmpty(groups) ? aggStr
                    : String.join(" and ", groups) + " and " + aggStr + " ";
        }

        @Override
        public String sql(QueryParam queryParam, boolean isOver, boolean asWith, String metricSql) {
            if (!asWith) {
                return String.format(
                        "select %s from ( select %s , %s from %s t0 left join %s t1 on %s ) metric_tb_src %s %s ",
                        getOverSelect(queryParam, isOver), getAllSelect(queryParam, "t0."),
                        getAllJoinSelect(queryParam, "t1."), metricSql, metricSql,
                        getJoinOn(queryParam, isOver, "t0.", "t1."),
                        getOrderBy(queryParam), getLimit(queryParam));
            }
            return String.format(
                    ",t0 as (select * from %s),t1 as (select * from %s) select %s from ( select %s , %s "
                            + "from  t0 left join t1 on %s ) metric_tb_src %s %s ",
                    metricSql, metricSql, getOverSelect(queryParam, isOver), getAllSelect(queryParam, "t0."),
                    getAllJoinSelect(queryParam, "t1."),
                    getJoinOn(queryParam, isOver, "t0.", "t1."),
                    getOrderBy(queryParam), getLimit(queryParam));
        }
    }

    public class MysqlEngineSql implements EngineSql {

        public String getTimeSpan(QueryParam queryParam, boolean isOver, boolean isAdd) {
            if (Objects.nonNull(queryParam.getDateInfo())) {
                String addStr = isAdd ? "" : "-";
                if (queryParam.getDateInfo().getPeriod().equalsIgnoreCase(Constants.DAY)) {
                    return isOver ? addStr + "7 day" : addStr + "1 day";
                }
                if (queryParam.getDateInfo().getPeriod().equalsIgnoreCase(Constants.WEEK)) {
                    return isOver ? addStr + "1 month" : addStr + "7 day";
                }
                if (queryParam.getDateInfo().getPeriod().equalsIgnoreCase(Constants.MONTH)) {
                    return isOver ? addStr + "1 year" : addStr + "1 month";
                }
            }
            return "";
        }

        public String getOverSelect(QueryParam queryParam, boolean isOver) {
            String aggStr = queryParam.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER) || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    return String.format(
                            "if(%s_roll!=0,  (%s-%s_roll)/%s_roll , 0) as %s_%s,%s",
                            f.getColumn(), f.getColumn(), f.getColumn(), f.getColumn(),
                            f.getColumn(), f.getFunc().getOperator(), f.getColumn());
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(","));
            return CollectionUtils.isEmpty(queryParam.getGroups()) ? aggStr
                    : String.join(",", queryParam.getGroups()) + "," + aggStr;
        }

        public String getJoinOn(QueryParam queryParam, boolean isOver, String aliasLeft, String aliasRight) {
            String timeDim = getTimeDim(queryParam);
            String timeSpan = "INTERVAL  " + getTimeSpan(queryParam, isOver, true);
            String aggStr = queryParam.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER) || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    if (queryParam.getDateInfo().getPeriod().equals(Constants.MONTH)) {
                        return String.format("%s = DATE_FORMAT(date_add(CONCAT(%s,'-01'), %s),'%%Y-%%m') ",
                                aliasLeft + timeDim, aliasRight + timeDim, timeSpan);
                    }
                    if (queryParam.getDateInfo().getPeriod().equals(Constants.WEEK) && isOver) {
                        return String.format("to_monday(date_add(%s ,INTERVAL %s) ) = %s",
                                aliasLeft + timeDim, getTimeSpan(queryParam, isOver, false), aliasRight + timeDim);
                    }
                    return String.format("%s = date_add(%s,%s) ",
                            aliasLeft + timeDim, aliasRight + timeDim, timeSpan);
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(" and "));
            List<String> groups = new ArrayList<>();
            for (String group : queryParam.getGroups()) {
                if (group.equalsIgnoreCase(timeDim)) {
                    continue;
                }
                groups.add(aliasLeft + group + " = " + aliasRight + group);
            }
            return CollectionUtils.isEmpty(groups) ? aggStr
                    : String.join(" and ", groups) + " and " + aggStr + " ";
        }

        @Override
        public String sql(QueryParam queryParam, boolean isOver, boolean asWith, String metricSql) {
            String sql = String.format(
                    "select %s from ( select %s , %s from %s t0 left join %s t1 on %s ) metric_tb_src %s %s ",
                    getOverSelect(queryParam, isOver), getAllSelect(queryParam, "t0."),
                    getAllJoinSelect(queryParam, "t1."), metricSql, metricSql,
                    getJoinOn(queryParam, isOver, "t0.", "t1."),
                    getOrderBy(queryParam), getLimit(queryParam));
            return sql;
        }
    }

    private String getAllJoinSelect(QueryParam queryParam, String alias) {
        String aggStr = queryParam.getAggregators().stream()
                .map(f -> getSelectField(f, alias) + " as " + getSelectField(f, "")
                        + "_roll")
                .collect(Collectors.joining(","));
        List<String> groups = new ArrayList<>();
        for (String group : queryParam.getGroups()) {
            groups.add(alias + group + " as " + group + "_roll");
        }
        return CollectionUtils.isEmpty(groups) ? aggStr
                : String.join(",", groups) + "," + aggStr;

    }

    private String getGroupDimWithOutTime(QueryParam queryParam) {
        String timeDim = getTimeDim(queryParam);
        return queryParam.getGroups().stream().filter(f -> !f.equalsIgnoreCase(timeDim))
                .collect(Collectors.joining(","));
    }

    private static String getTimeDim(QueryParam queryParam) {
        DateModeUtils dateModeUtils = ContextUtils.getContext().getBean(DateModeUtils.class);
        return dateModeUtils.getSysDateCol(queryParam.getDateInfo());
    }

    private static String getLimit(QueryParam queryParam) {
        if (queryParam != null && queryParam.getLimit() > 0) {
            return " limit " + String.valueOf(queryParam.getLimit());
        }
        return "";
    }

    private String getAllSelect(QueryParam queryParam, String alias) {
        String aggStr = queryParam.getAggregators().stream().map(f -> getSelectField(f, alias))
                .collect(Collectors.joining(","));
        return CollectionUtils.isEmpty(queryParam.getGroups()) ? aggStr
                : alias + String.join("," + alias, queryParam.getGroups()) + "," + aggStr;
    }

    private String getSelectField(final Aggregator agg, String alias) {
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        if (agg.getFunc().equals(AggOperatorEnum.RATIO_OVER) || agg.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
            return alias + agg.getColumn();
        }
        return sqlGenerateUtils.getSelectField(agg);
    }

    private String getGroupBy(QueryParam queryParam) {
        if (CollectionUtils.isEmpty(queryParam.getGroups())) {
            return "";
        }
        return "group by " + String.join(",", queryParam.getGroups());
    }

    private static String getOrderBy(QueryParam queryParam) {
        return "order by " + getTimeDim(queryParam) + " desc";
    }

    private boolean isOverRatio(QueryParam queryParam) {
        Long overCt = queryParam.getAggregators().stream()
                .filter(f -> f.getFunc().equals(AggOperatorEnum.RATIO_OVER)).count();
        return overCt > 0;
    }

    private void check(QueryParam queryParam) throws Exception {
        Long ratioOverNum = queryParam.getAggregators().stream()
                .filter(f -> f.getFunc().equals(AggOperatorEnum.RATIO_OVER)).count();
        Long ratioRollNum = queryParam.getAggregators().stream()
                .filter(f -> f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)).count();
        if (ratioOverNum > 0 && ratioRollNum > 0) {
            throw new Exception("not support over ratio and roll ratio together ");
        }
        if (getTimeDim(queryParam).isEmpty()) {
            throw new Exception("miss time filter");
        }

    }
}
