package com.tencent.supersonic.headless.core.translator.parser;

import com.tencent.supersonic.common.pojo.Aggregator;
import com.tencent.supersonic.common.pojo.enums.AggOperatorEnum;
import com.tencent.supersonic.common.pojo.enums.DatePeriodEnum;
import com.tencent.supersonic.common.pojo.enums.EngineType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.core.pojo.*;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component("MetricRatioParser")
@Slf4j
public class MetricRatioParser implements QueryParser {

    public interface EngineSql {

        String sql(StructQuery structQuery, boolean isOver, boolean asWith, String metricSql);
    }

    @Override
    public boolean accept(QueryStatement queryStatement) {
        if (Objects.isNull(queryStatement.getStructQuery()) || queryStatement.getIsS2SQL()
                || !isRatioAccept(queryStatement.getStructQuery())) {
            return false;
        }
        StructQuery structQuery = queryStatement.getStructQuery();
        if (structQuery.getQueryType().isNativeAggQuery()
                || CollectionUtils.isEmpty(structQuery.getAggregators())) {
            return false;
        }

        int nonSumFunction = 0;
        for (Aggregator agg : structQuery.getAggregators()) {
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
    public void parse(QueryStatement queryStatement) throws Exception {
        Ontology ontology = queryStatement.getOntology();
        generateRatioSql(queryStatement, ontology.getDatabaseType(), ontology.getDatabaseVersion());
    }

    /** Ratio */
    public boolean isRatioAccept(StructQuery structQuery) {
        Long ratioFuncNum = structQuery.getAggregators().stream()
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
        StructQuery structQuery = queryStatement.getStructQuery();
        check(structQuery);
        queryStatement.setEnableOptimize(false);
        OntologyQuery ontologyQuery = queryStatement.getOntologyQuery();
        ontologyQuery.setAggOption(AggOption.AGGREGATION);
        String metricTableName = "v_metric_tb_tmp";
        boolean isOver = isOverRatio(structQuery);
        String sql = "";

        SqlQuery sqlQuery = queryStatement.getSqlQuery();
        sqlQuery.setTable(metricTableName);
        switch (engineTypeEnum) {
            case H2:
                sql = new H2EngineSql().sql(structQuery, isOver, true, metricTableName);
                break;
            case MYSQL:
            case DORIS:
            case CLICKHOUSE:
                if (!sqlGenerateUtils.isSupportWith(engineTypeEnum, version)) {
                    sqlQuery.setSupportWith(false);
                }
                if (!engineTypeEnum.equals(engineTypeEnum.CLICKHOUSE)) {
                    sql = new MysqlEngineSql().sql(structQuery, isOver, sqlQuery.isSupportWith(),
                            metricTableName);
                } else {
                    sql = new CkEngineSql().sql(structQuery, isOver, sqlQuery.isSupportWith(),
                            metricTableName);
                }
                break;
            default:
        }
        sqlQuery.setSql(sql);
    }

    public class H2EngineSql implements EngineSql {

        public String getOverSelect(StructQuery structQuery, boolean isOver) {
            String aggStr = structQuery.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER)
                        || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    return String.format("( (%s-%s_roll)/cast(%s_roll as DOUBLE) ) as %s_%s,%s",
                            f.getColumn(), f.getColumn(), f.getColumn(), f.getColumn(),
                            f.getFunc().getOperator(), f.getColumn());
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(","));
            return CollectionUtils.isEmpty(structQuery.getGroups()) ? aggStr
                    : String.join(",", structQuery.getGroups()) + "," + aggStr;
        }

        public String getTimeSpan(StructQuery structQuery, boolean isOver, boolean isAdd) {
            if (Objects.nonNull(structQuery.getDateInfo())) {
                String addStr = isAdd ? "" : "-";
                if (structQuery.getDateInfo().getPeriod().equals(DatePeriodEnum.DAY)) {
                    return "day," + (isOver ? addStr + "7" : addStr + "1");
                }
                if (structQuery.getDateInfo().getPeriod().equals(DatePeriodEnum.MONTH)) {
                    return isOver ? "month," + addStr + "1" : "day," + addStr + "7";
                }
                if (structQuery.getDateInfo().getPeriod().equals(DatePeriodEnum.MONTH.MONTH)) {
                    return isOver ? "year," + addStr + "1" : "month," + addStr + "1";
                }
            }
            return "";
        }

        public String getJoinOn(StructQuery structQuery, boolean isOver, String aliasLeft,
                String aliasRight) {
            String timeDim = getTimeDim(structQuery);
            String timeSpan = getTimeSpan(structQuery, isOver, true);
            String aggStr = structQuery.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER)
                        || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    if (structQuery.getDateInfo().getPeriod().equals(DatePeriodEnum.MONTH)) {
                        return String.format(
                                "%s is not null and %s = FORMATDATETIME(DATEADD(%s,CONCAT(%s,'-01')),'yyyy-MM') ",
                                aliasRight + timeDim, aliasLeft + timeDim, timeSpan,
                                aliasRight + timeDim);
                    }
                    if (structQuery.getDateInfo().getPeriod().equals(DatePeriodEnum.WEEK)
                            && isOver) {
                        return String.format(" DATE_TRUNC('week',DATEADD(%s,%s) ) = %s ",
                                getTimeSpan(structQuery, isOver, false), aliasLeft + timeDim,
                                aliasRight + timeDim);
                    }
                    return String.format("%s = TIMESTAMPADD(%s,%s) ", aliasLeft + timeDim, timeSpan,
                            aliasRight + timeDim);
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(" and "));
            List<String> groups = new ArrayList<>();
            for (String group : structQuery.getGroups()) {
                if (group.equalsIgnoreCase(timeDim)) {
                    continue;
                }
                groups.add(aliasLeft + group + " = " + aliasRight + group);
            }
            return CollectionUtils.isEmpty(groups) ? aggStr
                    : String.join(" and ", groups) + " and " + aggStr + " ";
        }

        @Override
        public String sql(StructQuery structQuery, boolean isOver, boolean asWith,
                String metricSql) {
            String sql = String.format(
                    "select %s from ( select %s , %s from %s t0 left join %s t1 on %s ) metric_tb_src %s %s ",
                    getOverSelect(structQuery, isOver), getAllSelect(structQuery, "t0."),
                    getAllJoinSelect(structQuery, "t1."), metricSql, metricSql,
                    getJoinOn(structQuery, isOver, "t0.", "t1."), getOrderBy(structQuery),
                    getLimit(structQuery));
            return sql;
        }
    }

    public class CkEngineSql extends MysqlEngineSql {

        public String getJoinOn(StructQuery structQuery, boolean isOver, String aliasLeft,
                String aliasRight) {
            String timeDim = getTimeDim(structQuery);
            String timeSpan = "INTERVAL  " + getTimeSpan(structQuery, isOver, true);
            String aggStr = structQuery.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER)
                        || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    if (structQuery.getDateInfo().getPeriod().equals(DatePeriodEnum.MONTH)) {
                        return String.format(
                                "toDate(CONCAT(%s,'-01')) = date_add(toDate(CONCAT(%s,'-01')),%s)  ",
                                aliasLeft + timeDim, aliasRight + timeDim, timeSpan);
                    }
                    if (structQuery.getDateInfo().getPeriod().equals(DatePeriodEnum.WEEK)
                            && isOver) {
                        return String.format("toMonday(date_add(%s ,INTERVAL %s) ) = %s",
                                aliasLeft + timeDim, getTimeSpan(structQuery, isOver, false),
                                aliasRight + timeDim);
                    }
                    return String.format("%s = date_add(%s,%s) ", aliasLeft + timeDim,
                            aliasRight + timeDim, timeSpan);
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(" and "));
            List<String> groups = new ArrayList<>();
            for (String group : structQuery.getGroups()) {
                if (group.equalsIgnoreCase(timeDim)) {
                    continue;
                }
                groups.add(aliasLeft + group + " = " + aliasRight + group);
            }
            return CollectionUtils.isEmpty(groups) ? aggStr
                    : String.join(" and ", groups) + " and " + aggStr + " ";
        }

        @Override
        public String sql(StructQuery structQuery, boolean isOver, boolean asWith,
                String metricSql) {
            if (!asWith) {
                return String.format(
                        "select %s from ( select %s , %s from %s t0 left join %s t1 on %s ) metric_tb_src %s %s ",
                        getOverSelect(structQuery, isOver), getAllSelect(structQuery, "t0."),
                        getAllJoinSelect(structQuery, "t1."), metricSql, metricSql,
                        getJoinOn(structQuery, isOver, "t0.", "t1."), getOrderBy(structQuery),
                        getLimit(structQuery));
            }
            return String.format(
                    ",t0 as (select * from %s),t1 as (select * from %s) select %s from ( select %s , %s "
                            + "from  t0 left join t1 on %s ) metric_tb_src %s %s ",
                    metricSql, metricSql, getOverSelect(structQuery, isOver),
                    getAllSelect(structQuery, "t0."), getAllJoinSelect(structQuery, "t1."),
                    getJoinOn(structQuery, isOver, "t0.", "t1."), getOrderBy(structQuery),
                    getLimit(structQuery));
        }
    }

    public class MysqlEngineSql implements EngineSql {

        public String getTimeSpan(StructQuery structQuery, boolean isOver, boolean isAdd) {
            if (Objects.nonNull(structQuery.getDateInfo())) {
                String addStr = isAdd ? "" : "-";
                if (structQuery.getDateInfo().getPeriod().equals(DatePeriodEnum.DAY)) {
                    return isOver ? addStr + "7 day" : addStr + "1 day";
                }
                if (structQuery.getDateInfo().getPeriod().equals(DatePeriodEnum.WEEK)) {
                    return isOver ? addStr + "1 month" : addStr + "7 day";
                }
                if (structQuery.getDateInfo().getPeriod().equals(DatePeriodEnum.MONTH)) {
                    return isOver ? addStr + "1 year" : addStr + "1 month";
                }
            }
            return "";
        }

        public String getOverSelect(StructQuery structQuery, boolean isOver) {
            String aggStr = structQuery.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER)
                        || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    return String.format("if(%s_roll!=0,  (%s-%s_roll)/%s_roll , 0) as %s_%s,%s",
                            f.getColumn(), f.getColumn(), f.getColumn(), f.getColumn(),
                            f.getColumn(), f.getFunc().getOperator(), f.getColumn());
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(","));
            return CollectionUtils.isEmpty(structQuery.getGroups()) ? aggStr
                    : String.join(",", structQuery.getGroups()) + "," + aggStr;
        }

        public String getJoinOn(StructQuery structQuery, boolean isOver, String aliasLeft,
                String aliasRight) {
            String timeDim = getTimeDim(structQuery);
            String timeSpan = "INTERVAL  " + getTimeSpan(structQuery, isOver, true);
            String aggStr = structQuery.getAggregators().stream().map(f -> {
                if (f.getFunc().equals(AggOperatorEnum.RATIO_OVER)
                        || f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
                    if (structQuery.getDateInfo().getPeriod().equals(DatePeriodEnum.MONTH)) {
                        return String.format(
                                "%s = DATE_FORMAT(date_add(CONCAT(%s,'-01'), %s),'%%Y-%%m') ",
                                aliasLeft + timeDim, aliasRight + timeDim, timeSpan);
                    }
                    if (structQuery.getDateInfo().getPeriod().equals(DatePeriodEnum.WEEK)
                            && isOver) {
                        return String.format("to_monday(date_add(%s ,INTERVAL %s) ) = %s",
                                aliasLeft + timeDim, getTimeSpan(structQuery, isOver, false),
                                aliasRight + timeDim);
                    }
                    return String.format("%s = date_add(%s,%s) ", aliasLeft + timeDim,
                            aliasRight + timeDim, timeSpan);
                } else {
                    return f.getColumn();
                }
            }).collect(Collectors.joining(" and "));
            List<String> groups = new ArrayList<>();
            for (String group : structQuery.getGroups()) {
                if (group.equalsIgnoreCase(timeDim)) {
                    continue;
                }
                groups.add(aliasLeft + group + " = " + aliasRight + group);
            }
            return CollectionUtils.isEmpty(groups) ? aggStr
                    : String.join(" and ", groups) + " and " + aggStr + " ";
        }

        @Override
        public String sql(StructQuery structQuery, boolean isOver, boolean asWith,
                String metricSql) {
            String sql = String.format(
                    "select %s from ( select %s , %s from %s t0 left join %s t1 on %s ) metric_tb_src %s %s ",
                    getOverSelect(structQuery, isOver), getAllSelect(structQuery, "t0."),
                    getAllJoinSelect(structQuery, "t1."), metricSql, metricSql,
                    getJoinOn(structQuery, isOver, "t0.", "t1."), getOrderBy(structQuery),
                    getLimit(structQuery));
            return sql;
        }
    }

    private String getAllJoinSelect(StructQuery structQuery, String alias) {
        String aggStr = structQuery.getAggregators().stream()
                .map(f -> getSelectField(f, alias) + " as " + getSelectField(f, "") + "_roll")
                .collect(Collectors.joining(","));
        List<String> groups = new ArrayList<>();
        for (String group : structQuery.getGroups()) {
            groups.add(alias + group + " as " + group + "_roll");
        }
        return CollectionUtils.isEmpty(groups) ? aggStr : String.join(",", groups) + "," + aggStr;
    }

    private static String getTimeDim(StructQuery structQuery) {
        return structQuery.getDateInfo().getDateField();
    }

    private static String getLimit(StructQuery structQuery) {
        if (structQuery != null && structQuery.getLimit() != null && structQuery.getLimit() > 0) {
            return " limit " + String.valueOf(structQuery.getLimit());
        }
        return "";
    }

    private String getAllSelect(StructQuery structQuery, String alias) {
        String aggStr = structQuery.getAggregators().stream().map(f -> getSelectField(f, alias))
                .collect(Collectors.joining(","));
        return CollectionUtils.isEmpty(structQuery.getGroups()) ? aggStr
                : alias + String.join("," + alias, structQuery.getGroups()) + "," + aggStr;
    }

    private String getSelectField(final Aggregator agg, String alias) {
        SqlGenerateUtils sqlGenerateUtils = ContextUtils.getBean(SqlGenerateUtils.class);
        if (agg.getFunc().equals(AggOperatorEnum.RATIO_OVER)
                || agg.getFunc().equals(AggOperatorEnum.RATIO_ROLL)) {
            return alias + agg.getColumn();
        }
        return sqlGenerateUtils.getSelectField(agg);
    }

    private static String getOrderBy(StructQuery structQuery) {
        return "order by " + getTimeDim(structQuery) + " desc";
    }

    private boolean isOverRatio(StructQuery structQuery) {
        Long overCt = structQuery.getAggregators().stream()
                .filter(f -> f.getFunc().equals(AggOperatorEnum.RATIO_OVER)).count();
        return overCt > 0;
    }

    private void check(StructQuery structQuery) throws Exception {
        Long ratioOverNum = structQuery.getAggregators().stream()
                .filter(f -> f.getFunc().equals(AggOperatorEnum.RATIO_OVER)).count();
        Long ratioRollNum = structQuery.getAggregators().stream()
                .filter(f -> f.getFunc().equals(AggOperatorEnum.RATIO_ROLL)).count();
        if (ratioOverNum > 0 && ratioRollNum > 0) {
            throw new Exception("not support over ratio and roll ratio together ");
        }
        if (getTimeDim(structQuery).isEmpty()) {
            throw new Exception("miss time filter");
        }
    }
}
