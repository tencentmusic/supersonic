package com.tencent.supersonic.headless.core.translator.converter;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.core.pojo.Database;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.SqlQueryParam;
import com.tencent.supersonic.headless.core.pojo.StructQueryParam;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.OntologyQueryParam;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component("CalculateAggConverter")
@Slf4j
public class MetricRatioConverter implements QueryConverter {

    public interface EngineSql {

        String sql(StructQueryParam structQueryParam, boolean isOver, boolean asWith,
                String metricSql);
    }

    @Override
    public boolean accept(QueryStatement queryStatement) {
        if (Objects.isNull(queryStatement.getStructQueryParam()) || queryStatement.getIsS2SQL()
                || !isRatioAccept(queryStatement.getStructQueryParam())) {
            return false;
        }
        StructQueryParam structQueryParam = queryStatement.getStructQueryParam();
        if (structQueryParam.getQueryType().isNativeAggQuery()
                || CollectionUtils.isEmpty(structQueryParam.getAggregators())) {
            return false;
        }

        int nonSumFunction = 0;
        for (Aggregator agg : structQueryParam.getAggregators()) {
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
        Database database = queryStatement.getOntology().getDatabase();
        generateRatioSql(queryStatement, database.getType(), database.getVersion());
    }

    /** Ratio */
    public boolean isRatioAccept(StructQueryParam structQueryParam) {
        Long ratioFuncNum = structQueryParam.getAggregators().stream()
                .filter(f -> (f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)
                        || f.getFunc().equals(AggOperatorEnum.RATIO_OVER)))
                .count();
        if (ratioFuncNum > 0) {
            return true;
        }
        return false;
    }

    public void generateRatioSql(QueryStatement queryStatement, EngineType engineTypeEnum,
            String version) throws Exception {
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        StructQueryParam structQueryParam = queryStatement.getStructQueryParam();
        check(structQueryParam);
        queryStatement.setEnableOptimize(false);
        OntologyQueryParam ontologyQueryParam = queryStatement.getOntologyQueryParam();
        ontologyQueryParam.setAggOption(AggOption.AGGREGATION);
        String metricTableName = "v_metric_tb_tmp";
        boolean isOver = isOverRatio(structQueryParam);
        String sql = "";

        SqlQueryParam dsParam = queryStatement.getSqlQueryParam();
        dsParam.setTable(metricTableName);
        switch (engineTypeEnum) {
            case H2:
                sql = new H2EngineSql().sql(structQueryParam, isOver, true, metricTableName);
                break;
            case MYSQL:
            case DORIS:
            case CLICKHOUSE:
                if (!sqlGenerateUtils.isSupportWith(engineTypeEnum, version)) {
                    dsParam.setSupportWith(false);
                }
                if (!engineTypeEnum.equals(engineTypeEnum.CLICKHOUSE)) {
                    sql = new MysqlEngineSql().sql(structQueryParam, isOver,
                            dsParam.isSupportWith(), metricTableName);
                } else {
                    sql = new CkEngineSql().sql(structQueryParam, isOver, dsParam.isSupportWith(),
                            metricTableName);
                }
                break;
            default:
        }
        dsParam.setSql(sql);
    }

    public class H2EngineSql implements EngineSql {

        public String getOverSelect(StructQueryParam structQueryParam, boolean isOver) {
            String aggStr = structQueryParam.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER)
                        || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    return String.format("( (%s-%s_roll)/cast(%s_roll as DOUBLE) ) as %s_%s,%s",
                            f.getColumn(), f.getColumn(), f.getColumn(), f.getColumn(),
                            f.getFunc().getOperator(), f.getColumn());
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(","));
            return CollectionUtils.isEmpty(structQueryParam.getGroups()) ? aggStr
                    : String.join(",", structQueryParam.getGroups()) + "," + aggStr;
        }

        public String getTimeSpan(StructQueryParam structQueryParam, boolean isOver,
                boolean isAdd) {
            if (Objects.nonNull(structQueryParam.getDateInfo())) {
                String addStr = isAdd ? "" : "-";
                if (structQueryParam.getDateInfo().getPeriod().equals(DatePeriodEnum.DAY)) {
                    return "day," + (isOver ? addStr + "7" : addStr + "1");
                }
                if (structQueryParam.getDateInfo().getPeriod().equals(DatePeriodEnum.MONTH)) {
                    return isOver ? "month," + addStr + "1" : "day," + addStr + "7";
                }
                if (structQueryParam.getDateInfo().getPeriod().equals(DatePeriodEnum.MONTH.MONTH)) {
                    return isOver ? "year," + addStr + "1" : "month," + addStr + "1";
                }
            }
            return "";
        }

        public String getJoinOn(StructQueryParam structQueryParam, boolean isOver, String aliasLeft,
                String aliasRight) {
            String timeDim = getTimeDim(structQueryParam);
            String timeSpan = getTimeSpan(structQueryParam, isOver, true);
            String aggStr = structQueryParam.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER)
                        || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    if (structQueryParam.getDateInfo().getPeriod().equals(DatePeriodEnum.MONTH)) {
                        return String.format(
                                "%s is not null and %s = FORMATDATETIME(DATEADD(%s,CONCAT(%s,'-01')),'yyyy-MM') ",
                                aliasRight + timeDim, aliasLeft + timeDim, timeSpan,
                                aliasRight + timeDim);
                    }
                    if (structQueryParam.getDateInfo().getPeriod().equals(DatePeriodEnum.WEEK)
                            && isOver) {
                        return String.format(" DATE_TRUNC('week',DATEADD(%s,%s) ) = %s ",
                                getTimeSpan(structQueryParam, isOver, false), aliasLeft + timeDim,
                                aliasRight + timeDim);
                    }
                    return String.format("%s = TIMESTAMPADD(%s,%s) ", aliasLeft + timeDim, timeSpan,
                            aliasRight + timeDim);
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(" and "));
            List<String> groups = new ArrayList<>();
            for (String group : structQueryParam.getGroups()) {
                if (group.equalsIgnoreCase(timeDim)) {
                    continue;
                }
                groups.add(aliasLeft + group + " = " + aliasRight + group);
            }
            return CollectionUtils.isEmpty(groups) ? aggStr
                    : String.join(" and ", groups) + " and " + aggStr + " ";
        }

        @Override
        public String sql(StructQueryParam structQueryParam, boolean isOver, boolean asWith,
                String metricSql) {
            String sql = String.format(
                    "select %s from ( select %s , %s from %s t0 left join %s t1 on %s ) metric_tb_src %s %s ",
                    getOverSelect(structQueryParam, isOver), getAllSelect(structQueryParam, "t0."),
                    getAllJoinSelect(structQueryParam, "t1."), metricSql, metricSql,
                    getJoinOn(structQueryParam, isOver, "t0.", "t1."), getOrderBy(structQueryParam),
                    getLimit(structQueryParam));
            return sql;
        }
    }

    public class CkEngineSql extends MysqlEngineSql {

        public String getJoinOn(StructQueryParam structQueryParam, boolean isOver, String aliasLeft,
                String aliasRight) {
            String timeDim = getTimeDim(structQueryParam);
            String timeSpan = "INTERVAL  " + getTimeSpan(structQueryParam, isOver, true);
            String aggStr = structQueryParam.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER)
                        || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    if (structQueryParam.getDateInfo().getPeriod().equals(DatePeriodEnum.MONTH)) {
                        return String.format(
                                "toDate(CONCAT(%s,'-01')) = date_add(toDate(CONCAT(%s,'-01')),%s)  ",
                                aliasLeft + timeDim, aliasRight + timeDim, timeSpan);
                    }
                    if (structQueryParam.getDateInfo().getPeriod().equals(DatePeriodEnum.WEEK)
                            && isOver) {
                        return String.format("toMonday(date_add(%s ,INTERVAL %s) ) = %s",
                                aliasLeft + timeDim, getTimeSpan(structQueryParam, isOver, false),
                                aliasRight + timeDim);
                    }
                    return String.format("%s = date_add(%s,%s) ", aliasLeft + timeDim,
                            aliasRight + timeDim, timeSpan);
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(" and "));
            List<String> groups = new ArrayList<>();
            for (String group : structQueryParam.getGroups()) {
                if (group.equalsIgnoreCase(timeDim)) {
                    continue;
                }
                groups.add(aliasLeft + group + " = " + aliasRight + group);
            }
            return CollectionUtils.isEmpty(groups) ? aggStr
                    : String.join(" and ", groups) + " and " + aggStr + " ";
        }

        @Override
        public String sql(StructQueryParam structQueryParam, boolean isOver, boolean asWith,
                String metricSql) {
            if (!asWith) {
                return String.format(
                        "select %s from ( select %s , %s from %s t0 left join %s t1 on %s ) metric_tb_src %s %s ",
                        getOverSelect(structQueryParam, isOver),
                        getAllSelect(structQueryParam, "t0."),
                        getAllJoinSelect(structQueryParam, "t1."), metricSql, metricSql,
                        getJoinOn(structQueryParam, isOver, "t0.", "t1."),
                        getOrderBy(structQueryParam), getLimit(structQueryParam));
            }
            return String.format(
                    ",t0 as (select * from %s),t1 as (select * from %s) select %s from ( select %s , %s "
                            + "from  t0 left join t1 on %s ) metric_tb_src %s %s ",
                    metricSql, metricSql, getOverSelect(structQueryParam, isOver),
                    getAllSelect(structQueryParam, "t0."),
                    getAllJoinSelect(structQueryParam, "t1."),
                    getJoinOn(structQueryParam, isOver, "t0.", "t1."), getOrderBy(structQueryParam),
                    getLimit(structQueryParam));
        }
    }

    public class MysqlEngineSql implements EngineSql {

        public String getTimeSpan(StructQueryParam structQueryParam, boolean isOver,
                boolean isAdd) {
            if (Objects.nonNull(structQueryParam.getDateInfo())) {
                String addStr = isAdd ? "" : "-";
                if (structQueryParam.getDateInfo().getPeriod().equals(DatePeriodEnum.DAY)) {
                    return isOver ? addStr + "7 day" : addStr + "1 day";
                }
                if (structQueryParam.getDateInfo().getPeriod().equals(DatePeriodEnum.WEEK)) {
                    return isOver ? addStr + "1 month" : addStr + "7 day";
                }
                if (structQueryParam.getDateInfo().getPeriod().equals(DatePeriodEnum.MONTH)) {
                    return isOver ? addStr + "1 year" : addStr + "1 month";
                }
            }
            return "";
        }

        public String getOverSelect(StructQueryParam structQueryParam, boolean isOver) {
            String aggStr = structQueryParam.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER)
                        || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    return String.format("if(%s_roll!=0,  (%s-%s_roll)/%s_roll , 0) as %s_%s,%s",
                            f.getColumn(), f.getColumn(), f.getColumn(), f.getColumn(),
                            f.getColumn(), f.getFunc().getOperator(), f.getColumn());
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(","));
            return CollectionUtils.isEmpty(structQueryParam.getGroups()) ? aggStr
                    : String.join(",", structQueryParam.getGroups()) + "," + aggStr;
        }

        public String getJoinOn(StructQueryParam structQueryParam, boolean isOver, String aliasLeft,
                String aliasRight) {
            String timeDim = getTimeDim(structQueryParam);
            String timeSpan = "INTERVAL  " + getTimeSpan(structQueryParam, isOver, true);
            String aggStr = structQueryParam.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER)
                        || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    if (structQueryParam.getDateInfo().getPeriod().equals(DatePeriodEnum.MONTH)) {
                        return String.format(
                                "%s = DATE_FORMAT(date_add(CONCAT(%s,'-01'), %s),'%%Y-%%m') ",
                                aliasLeft + timeDim, aliasRight + timeDim, timeSpan);
                    }
                    if (structQueryParam.getDateInfo().getPeriod().equals(DatePeriodEnum.WEEK)
                            && isOver) {
                        return String.format("to_monday(date_add(%s ,INTERVAL %s) ) = %s",
                                aliasLeft + timeDim, getTimeSpan(structQueryParam, isOver, false),
                                aliasRight + timeDim);
                    }
                    return String.format("%s = date_add(%s,%s) ", aliasLeft + timeDim,
                            aliasRight + timeDim, timeSpan);
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(" and "));
            List<String> groups = new ArrayList<>();
            for (String group : structQueryParam.getGroups()) {
                if (group.equalsIgnoreCase(timeDim)) {
                    continue;
                }
                groups.add(aliasLeft + group + " = " + aliasRight + group);
            }
            return CollectionUtils.isEmpty(groups) ? aggStr
                    : String.join(" and ", groups) + " and " + aggStr + " ";
        }

        @Override
        public String sql(StructQueryParam structQueryParam, boolean isOver, boolean asWith,
                String metricSql) {
            String sql = String.format(
                    "select %s from ( select %s , %s from %s t0 left join %s t1 on %s ) metric_tb_src %s %s ",
                    getOverSelect(structQueryParam, isOver), getAllSelect(structQueryParam, "t0."),
                    getAllJoinSelect(structQueryParam, "t1."), metricSql, metricSql,
                    getJoinOn(structQueryParam, isOver, "t0.", "t1."), getOrderBy(structQueryParam),
                    getLimit(structQueryParam));
            return sql;
        }
    }

    private String getAllJoinSelect(StructQueryParam structQueryParam, String alias) {
        String aggStr = structQueryParam.getAggregators().stream()
                .map(f -> getSelectField(f, alias) + " as " + getSelectField(f, "") + "_roll")
                .collect(Collectors.joining(","));
        List<String> groups = new ArrayList<>();
        for (String group : structQueryParam.getGroups()) {
            groups.add(alias + group + " as " + group + "_roll");
        }
        return CollectionUtils.isEmpty(groups) ? aggStr : String.join(",", groups) + "," + aggStr;
    }

    private static String getTimeDim(StructQueryParam structQueryParam) {
        return structQueryParam.getDateInfo().getDateField();
    }

    private static String getLimit(StructQueryParam structQueryParam) {
        if (structQueryParam != null && structQueryParam.getLimit() != null
                && structQueryParam.getLimit() > 0) {
            return " limit " + String.valueOf(structQueryParam.getLimit());
        }
        return "";
    }

    private String getAllSelect(StructQueryParam structQueryParam, String alias) {
        String aggStr = structQueryParam.getAggregators().stream()
                .map(f -> getSelectField(f, alias)).collect(Collectors.joining(","));
        return CollectionUtils.isEmpty(structQueryParam.getGroups()) ? aggStr
                : alias + String.join("," + alias, structQueryParam.getGroups()) + "," + aggStr;
    }

    private String getSelectField(final Aggregator agg, String alias) {
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        if (agg.getFunc().equals(AggOperatorEnum.RATIO_OVER)
                || agg.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
            return alias + agg.getColumn();
        }
        return sqlGenerateUtils.getSelectField(agg);
    }

    private static String getOrderBy(StructQueryParam structQueryParam) {
        return "order by " + getTimeDim(structQueryParam) + " desc";
    }

    private boolean isOverRatio(StructQueryParam structQueryParam) {
        Long overCt = structQueryParam.getAggregators().stream()
                .filter(f -> f.getFunc().equals(AggOperatorEnum.RATIO_OVER)).count();
        return overCt > 0;
    }

    private void check(StructQueryParam structQueryParam) throws Exception {
        Long ratioOverNum = structQueryParam.getAggregators().stream()
                .filter(f -> f.getFunc().equals(AggOperatorEnum.RATIO_OVER)).count();
        Long ratioRollNum = structQueryParam.getAggregators().stream()
                .filter(f -> f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)).count();
        if (ratioOverNum > 0 && ratioRollNum > 0) {
            throw new Exception("not support over ratio and roll ratio together ");
        }
        if (getTimeDim(structQueryParam).isEmpty()) {
            throw new Exception("miss time filter");
        }
    }
}
