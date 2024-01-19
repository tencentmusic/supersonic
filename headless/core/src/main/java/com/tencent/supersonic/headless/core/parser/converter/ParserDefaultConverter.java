package com.tencent.supersonic.headless.core.parser.converter;

import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.headless.api.pojo.Param;
import com.tencent.supersonic.headless.api.request.MetricQueryReq;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.core.parser.HeadlessConverter;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.DataSource;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * HeadlessConverter default implement
 */
@Component("ParserDefaultConverter")
@Slf4j
public class ParserDefaultConverter implements HeadlessConverter {

    private final SqlGenerateUtils sqlGenerateUtils;

    private final CalculateAggConverter calculateConverterAgg;

    public ParserDefaultConverter(CalculateAggConverter calculateConverterAgg,
            SqlGenerateUtils sqlGenerateUtils) {
        this.calculateConverterAgg = calculateConverterAgg;
        this.sqlGenerateUtils = sqlGenerateUtils;
    }

    @Override
    public boolean accept(QueryStatement queryStatement) {
        if (Objects.isNull(queryStatement.getQueryStructReq()) || queryStatement.getIsS2SQL()) {
            return false;
        }
        return !calculateConverterAgg.accept(queryStatement);
    }

    @Override
    public void convert(QueryStatement queryStatement) throws Exception {
        QueryStructReq queryStructReq = queryStatement.getQueryStructReq();
        MetricQueryReq metricQueryReq = queryStatement.getMetricReq();
        MetricQueryReq metricReq = generateSqlCommand(queryStructReq, queryStatement);
        queryStatement.setMinMaxTime(sqlGenerateUtils.getBeginEndTime(queryStructReq, null));
        BeanUtils.copyProperties(metricReq, metricQueryReq);
    }

    public MetricQueryReq generateSqlCommand(QueryStructReq queryStructReq, QueryStatement queryStatement) {
        MetricQueryReq metricQueryReq = new MetricQueryReq();
        metricQueryReq.setMetrics(queryStructReq.getMetrics());
        metricQueryReq.setDimensions(queryStructReq.getGroups());
        String where = sqlGenerateUtils.generateWhere(queryStructReq, null);
        log.info("in generateSqlCommend, complete where:{}", where);

        metricQueryReq.setWhere(where);
        metricQueryReq.setOrder(queryStructReq.getOrders().stream()
                .map(order -> new ColumnOrder(order.getColumn(), order.getDirection())).collect(Collectors.toList()));
        metricQueryReq.setVariables(queryStructReq.getParams().stream()
                .collect(Collectors.toMap(Param::getName, Param::getValue, (k1, k2) -> k1)));
        metricQueryReq.setLimit(queryStructReq.getLimit());
        String rootPath = queryStructReq.getModelIdStr();
        metricQueryReq.setRootPath(rootPath);

        // support detail query
        if (queryStructReq.getQueryType().isNativeAggQuery() && CollectionUtils.isEmpty(metricQueryReq.getMetrics())) {
            Map<Long, DataSource> dataSourceMap = queryStatement.getSemanticModel().getModelMap();
            for (Long modelId : queryStructReq.getModelIds()) {
                String modelBizName = dataSourceMap.get(modelId).getName();
                String internalMetricName = sqlGenerateUtils.generateInternalMetricName(modelBizName);
                metricQueryReq.getMetrics().add(internalMetricName);
            }
        }

        return metricQueryReq;
    }

}
