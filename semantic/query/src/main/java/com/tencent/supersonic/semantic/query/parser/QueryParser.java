package com.tencent.supersonic.semantic.query.parser;

import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.semantic.api.query.enums.AggOption;
import com.tencent.supersonic.semantic.api.query.pojo.MetricTable;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
import com.tencent.supersonic.semantic.query.utils.ComponentFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
        metricReq.setNativeQuery(queryStructReq.getNativeQuery());
        return parser(metricReq);
    }


    public QueryStatement parser(ParseSqlReq sqlCommend) {
        log.info("parser MetricReq [{}] ", sqlCommend);
        QueryStatement queryStatement = new QueryStatement();
        try {
            if (!CollectionUtils.isEmpty(sqlCommend.getTables())) {
                List<String[]> tables = new ArrayList<>();
                String sourceId = "";
                for (MetricTable metricTable : sqlCommend.getTables()) {
                    MetricReq metricReq = new MetricReq();
                    metricReq.setMetrics(metricTable.getMetrics());
                    metricReq.setDimensions(metricTable.getDimensions());
                    metricReq.setWhere(StringUtil.formatSqlQuota(metricTable.getWhere()));
                    metricReq.setNativeQuery(!AggOption.isAgg(metricTable.getAggOption()));
                    metricReq.setRootPath(sqlCommend.getRootPath());
                    QueryStatement tableSql = parser(metricReq, metricTable.getAggOption());
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
                    if (sqlCommend.isSupportWith()) {
                        sql = "with " + String.join(",",
                                tables.stream().map(t -> String.format("%s as (%s)", t[0], t[1])).collect(
                                        Collectors.toList())) + "\n" + sqlCommend.getSql();
                    } else {
                        sql = sqlCommend.getSql();
                        for (String[] tb : tables) {
                            sql = StringUtils.replace(sql, tb[0],
                                    "(" + tb[1] + ") " + (sqlCommend.isWithAlias() ? "" : tb[0]), -1);
                        }
                    }
                    queryStatement.setSql(sql);
                    queryStatement.setSourceId(sourceId);
                    queryStatement.setParseSqlReq(sqlCommend);
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
        return parser(metricCommand, AggOption.getAggregation(metricCommand.isNativeQuery()));
    }

    public QueryStatement parser(MetricReq metricCommand, AggOption isAgg) {
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


}
