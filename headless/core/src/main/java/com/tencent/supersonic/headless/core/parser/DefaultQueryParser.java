package com.tencent.supersonic.headless.core.parser;

import com.google.common.base.Strings;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.headless.api.pojo.MetricTable;
import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.request.SqlExecuteReq;
import com.tencent.supersonic.headless.core.parser.converter.HeadlessConverter;
import com.tencent.supersonic.headless.core.pojo.MetricQueryParam;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.ViewQueryParam;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
@Primary
public class DefaultQueryParser implements QueryParser {

    public void parse(QueryStatement queryStatement) throws Exception {
        QueryParam queryParam = queryStatement.getQueryParam();
        if (Objects.isNull(queryStatement.getViewQueryParam())) {
            queryStatement.setViewQueryParam(new ViewQueryParam());
        }
        if (Objects.isNull(queryStatement.getMetricQueryParam())) {
            queryStatement.setMetricQueryParam(new MetricQueryParam());
        }
        log.info("SemanticConverter before [{}]", queryParam);
        for (HeadlessConverter headlessConverter : ComponentFactory.getSemanticConverters()) {
            if (headlessConverter.accept(queryStatement)) {
                log.info("SemanticConverter accept [{}]", headlessConverter.getClass().getName());
                headlessConverter.convert(queryStatement);
            }
        }
        log.info("SemanticConverter after {} {} {}", queryParam, queryStatement.getViewQueryParam(),
                queryStatement.getMetricQueryParam());
        if (!queryStatement.getViewQueryParam().getSql().isEmpty()) {
            queryStatement = parser(queryStatement.getViewQueryParam(), queryStatement);
        } else {
            queryStatement.getMetricQueryParam().setNativeQuery(queryParam.getQueryType().isNativeAggQuery());
            queryStatement = parser(queryStatement);
        }
        if (Strings.isNullOrEmpty(queryStatement.getSql())
                || Strings.isNullOrEmpty(queryStatement.getSourceId())) {
            throw new RuntimeException("parse Exception: " + queryStatement.getErrMsg());
        }
        String querySql =
                Objects.nonNull(queryStatement.getEnableLimitWrapper()) && queryStatement.getEnableLimitWrapper()
                        ? String.format(SqlExecuteReq.LIMIT_WRAPPER,
                        queryStatement.getSql())
                        : queryStatement.getSql();
        queryStatement.setSql(querySql);
    }

    public QueryStatement parser(ViewQueryParam viewQueryParam, QueryStatement queryStatement) {
        log.info("parser MetricReq [{}] ", viewQueryParam);
        try {
            if (!CollectionUtils.isEmpty(viewQueryParam.getTables())) {
                List<String[]> tables = new ArrayList<>();
                Boolean isSingleTable = viewQueryParam.getTables().size() == 1;
                for (MetricTable metricTable : viewQueryParam.getTables()) {
                    QueryStatement tableSql = parserSql(metricTable, isSingleTable, viewQueryParam, queryStatement);
                    if (isSingleTable && Objects.nonNull(tableSql.getViewSimplifySql())
                            && !tableSql.getViewSimplifySql().isEmpty()) {
                        queryStatement.setSql(tableSql.getViewSimplifySql());
                        queryStatement.setViewQueryParam(viewQueryParam);
                        return queryStatement;
                    }
                    tables.add(new String[]{metricTable.getAlias(), tableSql.getSql()});
                }
                if (!tables.isEmpty()) {
                    String sql = "";
                    if (viewQueryParam.isSupportWith()) {
                        sql = "with " + String.join(",",
                                tables.stream().map(t -> String.format("%s as (%s)", t[0], t[1])).collect(
                                        Collectors.toList())) + "\n" + viewQueryParam.getSql();
                    } else {
                        sql = viewQueryParam.getSql();
                        for (String[] tb : tables) {
                            sql = StringUtils.replace(sql, tb[0],
                                    "(" + tb[1] + ") " + (viewQueryParam.isWithAlias() ? "" : tb[0]), -1);
                        }
                    }
                    queryStatement.setSql(sql);
                    queryStatement.setViewQueryParam(viewQueryParam);
                    return queryStatement;
                }
            }
        } catch (Exception e) {
            log.error("physicalSql error {}", e);
            queryStatement.setErrMsg(e.getMessage());
        }
        return queryStatement;
    }

    public QueryStatement parser(QueryStatement queryStatement) {
        return parser(queryStatement, AggOption.getAggregation(queryStatement.getMetricQueryParam().isNativeQuery()));
    }

    public QueryStatement parser(QueryStatement queryStatement, AggOption isAgg) {
        MetricQueryParam metricQueryParam = queryStatement.getMetricQueryParam();
        log.info("parser metricQueryReq [{}] isAgg [{}]", metricQueryParam, isAgg);
        try {
            return ComponentFactory.getSqlParser().explain(queryStatement, isAgg);
        } catch (Exception e) {
            queryStatement.setErrMsg(e.getMessage());
            log.error("parser error metricQueryReq[{}] error [{}]", metricQueryParam, e);
        }
        return queryStatement;
    }

    private QueryStatement parserSql(MetricTable metricTable, Boolean isSingleMetricTable,
            ViewQueryParam viewQueryParam,
            QueryStatement queryStatement) throws Exception {
        MetricQueryParam metricReq = new MetricQueryParam();
        metricReq.setMetrics(metricTable.getMetrics());
        metricReq.setDimensions(metricTable.getDimensions());
        metricReq.setWhere(StringUtil.formatSqlQuota(metricTable.getWhere()));
        metricReq.setNativeQuery(!AggOption.isAgg(metricTable.getAggOption()));
        QueryStatement tableSql = new QueryStatement();
        tableSql.setIsS2SQL(false);
        tableSql.setMetricQueryParam(metricReq);
        tableSql.setMinMaxTime(queryStatement.getMinMaxTime());
        tableSql.setEnableOptimize(queryStatement.getEnableOptimize());
        tableSql.setViewId(queryStatement.getViewId());
        tableSql.setSemanticModel(queryStatement.getSemanticModel());
        if (isSingleMetricTable) {
            tableSql.setViewSql(viewQueryParam.getSql());
            tableSql.setViewAlias(metricTable.getAlias());
        }
        tableSql = parser(tableSql, metricTable.getAggOption());
        if (!tableSql.isOk()) {
            throw new Exception(String.format("parser table [%s] error [%s]", metricTable.getAlias(),
                    tableSql.getErrMsg()));
        }
        queryStatement.setSourceId(tableSql.getSourceId());
        return tableSql;
    }

}
