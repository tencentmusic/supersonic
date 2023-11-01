package com.tencent.supersonic.semantic.query.parser.convert;


import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.query.enums.AggOption;
import com.tencent.supersonic.semantic.api.query.pojo.MetricTable;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.model.domain.pojo.EngineTypeEnum;
import com.tencent.supersonic.semantic.query.parser.SemanticConverter;
import com.tencent.supersonic.semantic.query.service.SemanticQueryEngine;
import com.tencent.supersonic.common.util.DateModeUtils;
import com.tencent.supersonic.semantic.query.utils.QueryStructUtils;
import com.tencent.supersonic.semantic.query.utils.SqlGenerateUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


@Component("CalculateAggConverter")
@Slf4j
public class CalculateAggConverter implements SemanticConverter {


    private final SemanticQueryEngine parserService;
    private final QueryStructUtils queryStructUtils;
    private final SqlGenerateUtils sqlGenerateUtils;
    private final Catalog catalog;

    @Value("${metricParser.agg.default:sum}")
    private String metricAggDefault;


    public CalculateAggConverter(
            SemanticQueryEngine parserService,
            @Lazy QueryStructUtils queryStructUtils,
            SqlGenerateUtils sqlGenerateUtils, Catalog catalog) {
        this.parserService = parserService;
        this.queryStructUtils = queryStructUtils;
        this.sqlGenerateUtils = sqlGenerateUtils;
        this.catalog = catalog;
    }

    public interface EngineSql {

        String sql(QueryStructReq queryStructCmd, boolean isOver, boolean asWith, String metricSql);
    }

    public ParseSqlReq generateSqlCommend(QueryStructReq queryStructCmd, EngineTypeEnum engineTypeEnum, String version)
            throws Exception {
        // 同环比
        if (isRatioAccept(queryStructCmd)) {
            return generateRatioSqlCommand(queryStructCmd, engineTypeEnum, version);
        }
        ParseSqlReq sqlCommand = new ParseSqlReq();
        sqlCommand.setRootPath(catalog.getModelFullPath(queryStructCmd.getModelId()));
        String metricTableName = "v_metric_tb_tmp";
        MetricTable metricTable = new MetricTable();
        metricTable.setAlias(metricTableName);
        metricTable.setMetrics(queryStructCmd.getMetrics());
        metricTable.setDimensions(queryStructCmd.getGroups());
        String where = queryStructUtils.generateWhere(queryStructCmd);
        log.info("in generateSqlCommand, complete where:{}", where);
        metricTable.setWhere(where);
        metricTable.setAggOption(AggOption.AGGREGATION);
        sqlCommand.setTables(new ArrayList<>(Collections.singletonList(metricTable)));
        String sql = String.format("select %s from %s  %s %s %s", sqlGenerateUtils.getSelect(queryStructCmd),
                metricTableName,
                sqlGenerateUtils.getGroupBy(queryStructCmd), sqlGenerateUtils.getOrderBy(queryStructCmd),
                sqlGenerateUtils.getLimit(queryStructCmd));
        if (!queryStructUtils.isSupportWith(engineTypeEnum, version)) {
            sqlCommand.setSupportWith(false);
            sql = String.format("select %s from %s t0 %s %s %s", sqlGenerateUtils.getSelect(queryStructCmd),
                    metricTableName,
                    sqlGenerateUtils.getGroupBy(queryStructCmd), sqlGenerateUtils.getOrderBy(queryStructCmd),
                    sqlGenerateUtils.getLimit(queryStructCmd));
        }
        sqlCommand.setSql(sql);
        return sqlCommand;
    }


    @Override
    public boolean accept(QueryStructReq queryStructCmd) {
        if (queryStructCmd.getNativeQuery()) {
            return false;
        }
        if (CollectionUtils.isEmpty(queryStructCmd.getAggregators())) {
            return false;
        }

        int nonSumFunction = 0;
        for (Aggregator agg : queryStructCmd.getAggregators()) {
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
    public void converter(Catalog catalog, QueryStructReq queryStructCmd, ParseSqlReq sqlCommend,
            MetricReq metricCommand) throws Exception {
        DatabaseResp databaseResp = catalog.getDatabaseByModelId(queryStructCmd.getModelId());
        ParseSqlReq parseSqlReq = generateSqlCommend(queryStructCmd,
                EngineTypeEnum.valueOf(databaseResp.getType().toUpperCase()), databaseResp.getVersion());
        sqlCommend.setSql(parseSqlReq.getSql());
        sqlCommend.setTables(parseSqlReq.getTables());
        sqlCommend.setRootPath(parseSqlReq.getRootPath());
        sqlCommend.setVariables(parseSqlReq.getVariables());
        sqlCommend.setSupportWith(parseSqlReq.isSupportWith());
    }


    /**
     * Ratio
     */

    public boolean isRatioAccept(QueryStructReq queryStructCmd) {
        Long ratioFuncNum = queryStructCmd.getAggregators().stream()
                .filter(f -> (f.getFunc().equals(AggOperatorEnum.RATIO_ROLL) || f.getFunc()
                        .equals(AggOperatorEnum.RATIO_OVER))).count();
        if (ratioFuncNum > 0) {
            return true;
        }
        return false;
    }

    public ParseSqlReq generateRatioSqlCommand(QueryStructReq queryStructCmd, EngineTypeEnum engineTypeEnum,
            String version)
            throws Exception {
        check(queryStructCmd);
        ParseSqlReq sqlCommand = new ParseSqlReq();
        sqlCommand.setRootPath(catalog.getModelFullPath(queryStructCmd.getModelId()));
        String metricTableName = "v_metric_tb_tmp";
        MetricTable metricTable = new MetricTable();
        metricTable.setAlias(metricTableName);
        metricTable.setMetrics(queryStructCmd.getMetrics());
        metricTable.setDimensions(queryStructCmd.getGroups());
        String where = queryStructUtils.generateWhere(queryStructCmd);
        log.info("in generateSqlCommend, complete where:{}", where);
        metricTable.setWhere(where);
        metricTable.setAggOption(AggOption.AGGREGATION);
        sqlCommand.setTables(new ArrayList<>(Collections.singletonList(metricTable)));
        boolean isOver = isOverRatio(queryStructCmd);
        String sql = "";
        switch (engineTypeEnum) {
            case H2:
                sql = new H2EngineSql().sql(queryStructCmd, isOver, true, metricTableName);
                break;
            case MYSQL:
            case DORIS:
            case CLICKHOUSE:
                if (!queryStructUtils.isSupportWith(engineTypeEnum, version)) {
                    sqlCommand.setSupportWith(false);
                }
                if (!engineTypeEnum.equals(engineTypeEnum.CLICKHOUSE)) {
                    sql = new MysqlEngineSql().sql(queryStructCmd, isOver, sqlCommand.isSupportWith(), metricTableName);
                } else {
                    sql = new CkEngineSql().sql(queryStructCmd, isOver, sqlCommand.isSupportWith(), metricTableName);
                }
                break;
            default:
        }
        sqlCommand.setSql(sql);
        return sqlCommand;
    }

    public class H2EngineSql implements EngineSql {

        public String getOverSelect(QueryStructReq queryStructCmd, boolean isOver) {
            String aggStr = queryStructCmd.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER) || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    return String.format("( (%s-%s_roll)/cast(%s_roll as DOUBLE) ) as %s_%s,%s",
                            f.getColumn(), f.getColumn(), f.getColumn(), f.getColumn(),
                            f.getFunc().getOperator(), f.getColumn());
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(","));
            return CollectionUtils.isEmpty(queryStructCmd.getGroups()) ? aggStr
                    : String.join(",", queryStructCmd.getGroups()) + "," + aggStr;
        }

        public String getTimeSpan(QueryStructReq queryStructCmd, boolean isOver, boolean isAdd) {
            if (Objects.nonNull(queryStructCmd.getDateInfo())) {
                String addStr = isAdd ? "" : "-";
                if (queryStructCmd.getDateInfo().getPeriod().equalsIgnoreCase(Constants.DAY)) {
                    return "day," + (isOver ? addStr + "7" : addStr + "1");
                }
                if (queryStructCmd.getDateInfo().getPeriod().equalsIgnoreCase(Constants.WEEK)) {
                    return isOver ? "month," + addStr + "1" : "day," + addStr + "7";
                }
                if (queryStructCmd.getDateInfo().getPeriod().equalsIgnoreCase(Constants.MONTH)) {
                    return isOver ? "year," + addStr + "1" : "month," + addStr + "1";
                }
            }
            return "";
        }

        public String getJoinOn(QueryStructReq queryStructCmd, boolean isOver, String aliasLeft, String aliasRight) {
            String timeDim = getTimeDim(queryStructCmd);
            String timeSpan = getTimeSpan(queryStructCmd, isOver, true);
            String aggStr = queryStructCmd.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER) || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    if (queryStructCmd.getDateInfo().getPeriod().equals(Constants.MONTH)) {
                        return String.format(
                                "%s is not null and %s = FORMATDATETIME(DATEADD(%s,CONCAT(%s,'-01')),'yyyy-MM') ",
                                aliasRight + timeDim, aliasLeft + timeDim, timeSpan, aliasRight + timeDim);
                    }
                    if (queryStructCmd.getDateInfo().getPeriod().equals(Constants.WEEK) && isOver) {
                        return String.format(" DATE_TRUNC('week',DATEADD(%s,%s) ) = %s ",
                                getTimeSpan(queryStructCmd, isOver, false), aliasLeft + timeDim, aliasRight + timeDim);
                    }
                    return String.format("%s = TIMESTAMPADD(%s,%s) ",
                            aliasLeft + timeDim, timeSpan, aliasRight + timeDim);
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(" and "));
            List<String> groups = new ArrayList<>();
            for (String group : queryStructCmd.getGroups()) {
                if (group.equalsIgnoreCase(timeDim)) {
                    continue;
                }
                groups.add(aliasLeft + group + " = " + aliasRight + group);
            }
            return CollectionUtils.isEmpty(groups) ? aggStr
                    : String.join(" and ", groups) + " and " + aggStr + " ";
        }

        @Override
        public String sql(QueryStructReq queryStructCmd, boolean isOver, boolean asWith, String metricSql) {
            String sql = String.format(
                    "select %s from ( select %s , %s from %s t0 left join %s t1 on %s ) metric_tb_src %s %s ",
                    getOverSelect(queryStructCmd, isOver), getAllSelect(queryStructCmd, "t0."),
                    getAllJoinSelect(queryStructCmd, "t1."), metricSql, metricSql,
                    getJoinOn(queryStructCmd, isOver, "t0.", "t1."),
                    getOrderBy(queryStructCmd), getLimit(queryStructCmd));
            return sql;
        }
    }

    public class CkEngineSql extends MysqlEngineSql {

        public String getJoinOn(QueryStructReq queryStructCmd, boolean isOver, String aliasLeft, String aliasRight) {
            String timeDim = getTimeDim(queryStructCmd);
            String timeSpan = "INTERVAL  " + getTimeSpan(queryStructCmd, isOver, true);
            String aggStr = queryStructCmd.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER) || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    if (queryStructCmd.getDateInfo().getPeriod().equals(Constants.MONTH)) {
                        return String.format("toDate(CONCAT(%s,'-01')) = date_add(toDate(CONCAT(%s,'-01')),%s)  ",
                                aliasLeft + timeDim, aliasRight + timeDim, timeSpan);
                    }
                    if (queryStructCmd.getDateInfo().getPeriod().equals(Constants.WEEK) && isOver) {
                        return String.format("toMonday(date_add(%s ,INTERVAL %s) ) = %s",
                                aliasLeft + timeDim, getTimeSpan(queryStructCmd, isOver, false), aliasRight + timeDim);
                    }
                    return String.format("%s = date_add(%s,%s) ",
                            aliasLeft + timeDim, aliasRight + timeDim, timeSpan);
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(" and "));
            List<String> groups = new ArrayList<>();
            for (String group : queryStructCmd.getGroups()) {
                if (group.equalsIgnoreCase(timeDim)) {
                    continue;
                }
                groups.add(aliasLeft + group + " = " + aliasRight + group);
            }
            return CollectionUtils.isEmpty(groups) ? aggStr
                    : String.join(" and ", groups) + " and " + aggStr + " ";
        }

        @Override
        public String sql(QueryStructReq queryStructCmd, boolean isOver, boolean asWith, String metricSql) {
            if (!asWith) {
                return String.format(
                        "select %s from ( select %s , %s from %s t0 left join %s t1 on %s ) metric_tb_src %s %s ",
                        getOverSelect(queryStructCmd, isOver), getAllSelect(queryStructCmd, "t0."),
                        getAllJoinSelect(queryStructCmd, "t1."), metricSql, metricSql,
                        getJoinOn(queryStructCmd, isOver, "t0.", "t1."),
                        getOrderBy(queryStructCmd), getLimit(queryStructCmd));
            }
            return String.format(
                    ",t0 as (select * from %s),t1 as (select * from %s) select %s from ( select %s , %s "
                            + "from  t0 left join t1 on %s ) metric_tb_src %s %s ",
                    metricSql, metricSql, getOverSelect(queryStructCmd, isOver), getAllSelect(queryStructCmd, "t0."),
                    getAllJoinSelect(queryStructCmd, "t1."),
                    getJoinOn(queryStructCmd, isOver, "t0.", "t1."),
                    getOrderBy(queryStructCmd), getLimit(queryStructCmd));
        }
    }

    public class MysqlEngineSql implements EngineSql {

        public String getTimeSpan(QueryStructReq queryStructCmd, boolean isOver, boolean isAdd) {
            if (Objects.nonNull(queryStructCmd.getDateInfo())) {
                String addStr = isAdd ? "" : "-";
                if (queryStructCmd.getDateInfo().getPeriod().equalsIgnoreCase(Constants.DAY)) {
                    return isOver ? addStr + "7 day" : addStr + "1 day";
                }
                if (queryStructCmd.getDateInfo().getPeriod().equalsIgnoreCase(Constants.WEEK)) {
                    return isOver ? addStr + "1 month" : addStr + "7 day";
                }
                if (queryStructCmd.getDateInfo().getPeriod().equalsIgnoreCase(Constants.MONTH)) {
                    return isOver ? addStr + "1 year" : addStr + "1 month";
                }
            }
            return "";
        }

        public String getOverSelect(QueryStructReq queryStructCmd, boolean isOver) {
            String aggStr = queryStructCmd.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER) || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    return String.format(
                            "if(%s_roll!=0,  (%s-%s_roll)/%s_roll , 0) as %s_%s,%s",
                            f.getColumn(), f.getColumn(), f.getColumn(), f.getColumn(),
                            f.getColumn(), f.getFunc().getOperator(), f.getColumn());
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(","));
            return CollectionUtils.isEmpty(queryStructCmd.getGroups()) ? aggStr
                    : String.join(",", queryStructCmd.getGroups()) + "," + aggStr;
        }

        public String getJoinOn(QueryStructReq queryStructCmd, boolean isOver, String aliasLeft, String aliasRight) {
            String timeDim = getTimeDim(queryStructCmd);
            String timeSpan = "INTERVAL  " + getTimeSpan(queryStructCmd, isOver, true);
            String aggStr = queryStructCmd.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER) || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    if (queryStructCmd.getDateInfo().getPeriod().equals(Constants.MONTH)) {
                        return String.format("%s = DATE_FORMAT(date_add(CONCAT(%s,'-01'), %s),'%%Y-%%m') ",
                                aliasLeft + timeDim, aliasRight + timeDim, timeSpan);
                    }
                    if (queryStructCmd.getDateInfo().getPeriod().equals(Constants.WEEK) && isOver) {
                        return String.format("to_monday(date_add(%s ,INTERVAL %s) ) = %s",
                                aliasLeft + timeDim, getTimeSpan(queryStructCmd, isOver, false), aliasRight + timeDim);
                    }
                    return String.format("%s = date_add(%s,%s) ",
                            aliasLeft + timeDim, aliasRight + timeDim, timeSpan);
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(" and "));
            List<String> groups = new ArrayList<>();
            for (String group : queryStructCmd.getGroups()) {
                if (group.equalsIgnoreCase(timeDim)) {
                    continue;
                }
                groups.add(aliasLeft + group + " = " + aliasRight + group);
            }
            return CollectionUtils.isEmpty(groups) ? aggStr
                    : String.join(" and ", groups) + " and " + aggStr + " ";
        }

        @Override
        public String sql(QueryStructReq queryStructCmd, boolean isOver, boolean asWith, String metricSql) {
            String sql = String.format(
                    "select %s from ( select %s , %s from %s t0 left join %s t1 on %s ) metric_tb_src %s %s ",
                    getOverSelect(queryStructCmd, isOver), getAllSelect(queryStructCmd, "t0."),
                    getAllJoinSelect(queryStructCmd, "t1."), metricSql, metricSql,
                    getJoinOn(queryStructCmd, isOver, "t0.", "t1."),
                    getOrderBy(queryStructCmd), getLimit(queryStructCmd));
            return sql;
        }
    }


    private String getAllJoinSelect(QueryStructReq queryStructCmd, String alias) {
        String aggStr = queryStructCmd.getAggregators().stream()
                .map(f -> getSelectField(f, alias) + " as " + getSelectField(f, "")
                        + "_roll")
                .collect(Collectors.joining(","));
        List<String> groups = new ArrayList<>();
        for (String group : queryStructCmd.getGroups()) {
            groups.add(alias + group + " as " + group + "_roll");
        }
        return CollectionUtils.isEmpty(groups) ? aggStr
                : String.join(",", groups) + "," + aggStr;

    }


    private String getGroupDimWithOutTime(QueryStructReq queryStructCmd) {
        String timeDim = getTimeDim(queryStructCmd);
        return queryStructCmd.getGroups().stream().filter(f -> !f.equalsIgnoreCase(timeDim))
                .collect(Collectors.joining(","));
    }

    private static String getTimeDim(QueryStructReq queryStructCmd) {
        DateModeUtils dateModeUtils = ContextUtils.getContext().getBean(DateModeUtils.class);
        return dateModeUtils.getSysDateCol(queryStructCmd.getDateInfo());
    }

    private static String getLimit(QueryStructReq queryStructCmd) {
        if (queryStructCmd != null && queryStructCmd.getLimit() > 0) {
            return " limit " + String.valueOf(queryStructCmd.getLimit());
        }
        return "";
    }


    private String getAllSelect(QueryStructReq queryStructCmd, String alias) {
        String aggStr = queryStructCmd.getAggregators().stream().map(f -> getSelectField(f, alias))
                .collect(Collectors.joining(","));
        return CollectionUtils.isEmpty(queryStructCmd.getGroups()) ? aggStr
                : alias + String.join("," + alias, queryStructCmd.getGroups()) + "," + aggStr;
    }

    private String getSelectField(final Aggregator agg, String alias) {
        if (agg.getFunc().equals(AggOperatorEnum.RATIO_OVER) || agg.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
            return alias + agg.getColumn();
        }
        return sqlGenerateUtils.getSelectField(agg);
    }

    private String getGroupBy(QueryStructReq queryStructCmd) {
        if (CollectionUtils.isEmpty(queryStructCmd.getGroups())) {
            return "";
        }
        return "group by " + String.join(",", queryStructCmd.getGroups());
    }

    private static String getOrderBy(QueryStructReq queryStructCmd) {
        return "order by " + getTimeDim(queryStructCmd) + " desc";
    }

    private boolean isOverRatio(QueryStructReq queryStructCmd) {
        Long overCt = queryStructCmd.getAggregators().stream()
                .filter(f -> f.getFunc().equals(AggOperatorEnum.RATIO_OVER)).count();
        return overCt > 0;
    }

    private void check(QueryStructReq queryStructCmd) throws Exception {
        Long ratioOverNum = queryStructCmd.getAggregators().stream()
                .filter(f -> f.getFunc().equals(AggOperatorEnum.RATIO_OVER)).count();
        Long ratioRollNum = queryStructCmd.getAggregators().stream()
                .filter(f -> f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)).count();
        if (ratioOverNum > 0 && ratioRollNum > 0) {
            throw new Exception("not support over ratio and roll ratio together ");
        }
        if (getTimeDim(queryStructCmd).isEmpty()) {
            throw new Exception("miss time filter");
        }

    }
}
