package com.tencent.supersonic.semantic.query.application.parser;

import com.tencent.supersonic.semantic.api.query.pojo.MetricTable;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.core.domain.Catalog;
import com.tencent.supersonic.semantic.query.domain.pojo.QueryStatement;
import com.tencent.supersonic.semantic.query.domain.utils.ComponentFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
@Primary
public class QueryParser {


    private final Catalog catalog;

    public QueryParser(Catalog catalog) {
        this.catalog = catalog;
    }

    public QueryStatement logicSql(QueryStructReq queryStructReq) throws Exception {
        ParseSqlReq parseSqlReq = new ParseSqlReq();
        MetricReq metricReq = new MetricReq();
        log.info("SemanticConverter before [{}]", queryStructReq);
        for (SemanticConverter semanticConverter : ComponentFactory.getSemanticConverters()) {
            if (semanticConverter.accept(queryStructReq)) {
                log.info("SemanticConverter accept [{}]", semanticConverter.getClass().getName());
                semanticConverter.converter(catalog, queryStructReq, parseSqlReq, metricReq);
            }
        }
        log.info("SemanticConverter after {} {} {}", queryStructReq, metricReq, parseSqlReq);
        if (!parseSqlReq.getSql().isEmpty()) {
            return parser(parseSqlReq);
        }
        return parser(metricReq);
    }


    public QueryStatement parser(ParseSqlReq sqlCommend) {
        log.info("parser MetricReq [{}] ", sqlCommend);
        QueryStatement queryStatement = new QueryStatement();
        try {
            if (!CollectionUtils.isEmpty(sqlCommend.getTables())) {
                List<String> tables = new ArrayList<>();
                String sourceId = "";
                for (MetricTable metricTable : sqlCommend.getTables()) {
                    MetricReq metricReq = new MetricReq();
                    metricReq.setMetrics(metricTable.getMetrics());
                    metricReq.setDimensions(metricTable.getDimensions());
                    metricReq.setWhere(formatWhere(metricTable.getWhere()));
                    metricReq.setRootPath(sqlCommend.getRootPath());
                    QueryStatement tableSql = parser(metricReq, metricTable.isAgg());
                    if (!tableSql.isOk()) {
                        queryStatement.setErrMsg(String.format("parser table [%s] error [%s]", metricTable.getAlias(),
                                tableSql.getErrMsg()));
                        return queryStatement;
                    }
                    tables.add(String.format("%s as (%s)", metricTable.getAlias(), tableSql.getSql()));
                    sourceId = tableSql.getSourceId();
                }

                if (!tables.isEmpty()) {
                    String sql = "with " + String.join(",", tables) + "\n" + sqlCommend.getSql();
                    queryStatement.setSql(sql);
                    queryStatement.setSourceId(sourceId);
                    return queryStatement;
                }
            }
        } catch (Exception e) {
            log.error("physicalSql error {}", e);
            queryStatement.setErrMsg(e.getMessage());
        }
        return queryStatement;
    }

    public QueryStatement parser(MetricReq metricCommand) {
        return parser(metricCommand, true);
    }

    public QueryStatement parser(MetricReq metricCommand, boolean isAgg) {
        log.info("parser MetricReq [{}] isAgg [{}]", metricCommand, isAgg);
        QueryStatement queryStatement = new QueryStatement();
        if (metricCommand.getRootPath().isEmpty()) {
            queryStatement.setErrMsg("rootPath empty");
            return queryStatement;
        }
        try {
            queryStatement = ComponentFactory.getSqlParser().explain(metricCommand, isAgg, catalog);
            return queryStatement;
        } catch (Exception e) {
            queryStatement.setErrMsg(e.getMessage());
            log.error("parser error MetricCommand[{}] error [{}]", metricCommand, e);
        }
        return queryStatement;
    }


    private String formatWhere(String where) {
        if (StringUtils.isEmpty(where)) {
            return where;
        }
        return where.replace("\"", "\\\\\"");
    }
}
