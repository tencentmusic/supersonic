package com.tencent.supersonic.headless.core.parser;

import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.headless.api.enums.AggOption;
import com.tencent.supersonic.headless.api.pojo.MetricTable;
import com.tencent.supersonic.headless.api.request.MetricQueryReq;
import com.tencent.supersonic.headless.api.request.ParseSqlReq;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
@Primary
public class QueryParser {

    public QueryStatement logicSql(QueryStatement queryStatement) throws Exception {
        QueryStructReq queryStructReq = queryStatement.getQueryStructReq();
        if (Objects.isNull(queryStatement.getParseSqlReq())) {
            queryStatement.setParseSqlReq(new ParseSqlReq());
        }
        if (Objects.isNull(queryStatement.getMetricReq())) {
            queryStatement.setMetricReq(new MetricQueryReq());
        }
        log.info("SemanticConverter before [{}]", queryStructReq);
        for (HeadlessConverter headlessConverter : ComponentFactory.getSemanticConverters()) {
            if (headlessConverter.accept(queryStatement)) {
                log.info("SemanticConverter accept [{}]", headlessConverter.getClass().getName());
                headlessConverter.convert(queryStatement);
            }
        }
        log.info("SemanticConverter after {} {} {}", queryStructReq, queryStatement.getParseSqlReq(),
                queryStatement.getMetricReq());
        if (!queryStatement.getParseSqlReq().getSql().isEmpty()) {
            return parser(queryStatement.getParseSqlReq(), queryStatement);
        }

        queryStatement.getMetricReq().setNativeQuery(queryStructReq.getQueryType().isNativeAggQuery());
        return parser(queryStatement);

    }

    public QueryStatement parser(ParseSqlReq parseSqlReq, QueryStatement queryStatement) {
        log.info("parser MetricReq [{}] ", parseSqlReq);
        try {
            if (!CollectionUtils.isEmpty(parseSqlReq.getTables())) {
                List<String[]> tables = new ArrayList<>();
                String sourceId = "";
                for (MetricTable metricTable : parseSqlReq.getTables()) {
                    MetricQueryReq metricReq = new MetricQueryReq();
                    metricReq.setMetrics(metricTable.getMetrics());
                    metricReq.setDimensions(metricTable.getDimensions());
                    metricReq.setWhere(StringUtil.formatSqlQuota(metricTable.getWhere()));
                    metricReq.setNativeQuery(!AggOption.isAgg(metricTable.getAggOption()));
                    metricReq.setRootPath(parseSqlReq.getRootPath());
                    QueryStatement tableSql = new QueryStatement();
                    tableSql.setIsS2SQL(false);
                    tableSql.setMetricReq(metricReq);
                    tableSql.setMinMaxTime(queryStatement.getMinMaxTime());
                    tableSql.setEnableOptimize(queryStatement.getEnableOptimize());
                    tableSql.setModelIds(queryStatement.getModelIds());
                    tableSql.setHeadlessModel(queryStatement.getHeadlessModel());
                    tableSql = parser(tableSql, metricTable.getAggOption());
                    if (!tableSql.isOk()) {
                        queryStatement.setErrMsg(String.format("parser table [%s] error [%s]", metricTable.getAlias(),
                                tableSql.getErrMsg()));
                        return queryStatement;
                    }
                    tables.add(new String[]{metricTable.getAlias(), tableSql.getSql()});
                    sourceId = tableSql.getSourceId();
                }

                if (!tables.isEmpty()) {
                    String sql = "";
                    if (parseSqlReq.isSupportWith()) {
                        sql = "with " + String.join(",",
                                tables.stream().map(t -> String.format("%s as (%s)", t[0], t[1])).collect(
                                        Collectors.toList())) + "\n" + parseSqlReq.getSql();
                    } else {
                        sql = parseSqlReq.getSql();
                        for (String[] tb : tables) {
                            sql = StringUtils.replace(sql, tb[0],
                                    "(" + tb[1] + ") " + (parseSqlReq.isWithAlias() ? "" : tb[0]), -1);
                        }
                    }
                    queryStatement.setSql(sql);
                    queryStatement.setSourceId(sourceId);
                    queryStatement.setParseSqlReq(parseSqlReq);
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
        return parser(queryStatement, AggOption.getAggregation(queryStatement.getMetricReq().isNativeQuery()));
    }

    public QueryStatement parser(QueryStatement queryStatement, AggOption isAgg) {
        MetricQueryReq metricQueryReq = queryStatement.getMetricReq();
        log.info("parser metricQueryReq [{}] isAgg [{}]", metricQueryReq, isAgg);
        if (metricQueryReq.getRootPath().isEmpty()) {
            queryStatement.setErrMsg("rootPath empty");
            return queryStatement;
        }
        try {
            queryStatement = ComponentFactory.getSqlParser().explain(queryStatement, isAgg);
            return queryStatement;
        } catch (Exception e) {
            queryStatement.setErrMsg(e.getMessage());
            log.error("parser error metricQueryReq[{}] error [{}]", metricQueryReq, e);
        }
        return queryStatement;
    }

}
