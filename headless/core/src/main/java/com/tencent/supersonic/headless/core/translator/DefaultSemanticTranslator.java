package com.tencent.supersonic.headless.core.translator;

import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.headless.api.pojo.MetricTable;
import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.api.pojo.enums.AggOption;
import com.tencent.supersonic.headless.core.pojo.DataSetQueryParam;
import com.tencent.supersonic.headless.core.pojo.MetricQueryParam;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.translator.converter.QueryConverter;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DefaultSemanticTranslator implements SemanticTranslator {

    public void translate(QueryStatement queryStatement) {
        try {
            parse(queryStatement);
            optimize(queryStatement);
            queryStatement.setOk(true);
        } catch (Exception e) {
            queryStatement.setOk(false);
        }
    }

    public void optimize(QueryStatement queryStatement) {
        for (QueryOptimizer queryOptimizer : ComponentFactory.getQueryOptimizers()) {
            queryOptimizer.rewrite(queryStatement);
        }
    }

    public void parse(QueryStatement queryStatement) throws Exception {
        QueryParam queryParam = queryStatement.getQueryParam();
        if (Objects.isNull(queryStatement.getDataSetQueryParam())) {
            queryStatement.setDataSetQueryParam(new DataSetQueryParam());
        }
        if (Objects.isNull(queryStatement.getMetricQueryParam())) {
            queryStatement.setMetricQueryParam(new MetricQueryParam());
        }
        log.debug("SemanticConverter before [{}]", queryParam);
        for (QueryConverter headlessConverter : ComponentFactory.getQueryConverters()) {
            if (headlessConverter.accept(queryStatement)) {
                log.debug("SemanticConverter accept [{}]", headlessConverter.getClass().getName());
                headlessConverter.convert(queryStatement);
            }
        }
        log.debug("SemanticConverter after {} {} {}", queryParam, queryStatement.getDataSetQueryParam(),
                queryStatement.getMetricQueryParam());
        if (!queryStatement.getDataSetQueryParam().getSql().isEmpty()) {
            doParse(queryStatement.getDataSetQueryParam(), queryStatement);
        } else {
            queryStatement.getMetricQueryParam().setNativeQuery(queryParam.getQueryType().isNativeAggQuery());
            doParse(queryStatement);
        }
        if (StringUtils.isEmpty(queryStatement.getSql())
                || StringUtils.isEmpty(queryStatement.getSourceId())) {
            throw new RuntimeException("parse Exception: " + queryStatement.getErrMsg());
        }
        if (StringUtils.isNotBlank(queryStatement.getSql())
                && !SqlSelectHelper.hasLimit(queryStatement.getSql())) {
            String querySql = queryStatement.getSql() + " limit " + queryStatement.getLimit().toString();
            queryStatement.setSql(querySql);
        }
    }

    public QueryStatement doParse(DataSetQueryParam dataSetQueryParam, QueryStatement queryStatement) {
        log.info("parse dataSetQuery [{}] ", dataSetQueryParam);
        try {
            if (!CollectionUtils.isEmpty(dataSetQueryParam.getTables())) {
                List<String[]> tables = new ArrayList<>();
                boolean isSingleTable = dataSetQueryParam.getTables().size() == 1;
                for (MetricTable metricTable : dataSetQueryParam.getTables()) {
                    QueryStatement tableSql = parserSql(metricTable, isSingleTable,
                            dataSetQueryParam, queryStatement);
                    if (isSingleTable && Objects.nonNull(tableSql.getDataSetQueryParam())
                            && !tableSql.getDataSetSimplifySql().isEmpty()) {
                        queryStatement.setSql(tableSql.getDataSetSimplifySql());
                        queryStatement.setDataSetQueryParam(dataSetQueryParam);
                        return queryStatement;
                    }
                    tables.add(new String[]{metricTable.getAlias(), tableSql.getSql()});
                }
                if (!tables.isEmpty()) {
                    String sql;
                    if (dataSetQueryParam.isSupportWith()) {
                        sql = "with " + tables.stream().map(t -> String.format("%s as (%s)", t[0], t[1])).collect(
                                Collectors.joining(",")) + "\n" + dataSetQueryParam.getSql();
                    } else {
                        sql = dataSetQueryParam.getSql();
                        for (String[] tb : tables) {
                            sql = StringUtils.replace(sql, tb[0],
                                    "(" + tb[1] + ") " + (dataSetQueryParam.isWithAlias() ? "" : tb[0]), -1);
                        }
                    }
                    queryStatement.setSql(sql);
                    queryStatement.setDataSetQueryParam(dataSetQueryParam);
                    return queryStatement;
                }
            }
        } catch (Exception e) {
            log.error("physicalSql error {}", e);
            queryStatement.setErrMsg(e.getMessage());
        }
        return queryStatement;
    }

    public QueryStatement doParse(QueryStatement queryStatement) {
        return doParse(queryStatement, AggOption.getAggregation(
                queryStatement.getMetricQueryParam().isNativeQuery()));
    }

    public QueryStatement doParse(QueryStatement queryStatement, AggOption isAgg) {
        MetricQueryParam metricQueryParam = queryStatement.getMetricQueryParam();
        log.info("parse metricQuery [{}] isAgg [{}]", metricQueryParam, isAgg);
        try {
            ComponentFactory.getQueryParser().parse(queryStatement, isAgg);
        } catch (Exception e) {
            queryStatement.setErrMsg(e.getMessage());
            log.error("parser error metricQueryReq[{}] error [{}]", metricQueryParam, e);
        }
        return queryStatement;
    }

    private QueryStatement parserSql(MetricTable metricTable, Boolean isSingleMetricTable,
                                     DataSetQueryParam dataSetQueryParam,
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
        tableSql.setDataSetId(queryStatement.getDataSetId());
        tableSql.setSemanticModel(queryStatement.getSemanticModel());
        if (isSingleMetricTable) {
            tableSql.setDataSetSql(dataSetQueryParam.getSql());
            tableSql.setDataSetAlias(metricTable.getAlias());
        }
        tableSql = doParse(tableSql, metricTable.getAggOption());
        if (!tableSql.isOk()) {
            throw new Exception(String.format("parser table [%s] error [%s]", metricTable.getAlias(),
                    tableSql.getErrMsg()));
        }
        queryStatement.setSourceId(tableSql.getSourceId());
        return tableSql;
    }

}
