package com.tencent.supersonic.headless.core.parser.converter;

import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.headless.api.pojo.Param;
import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.core.pojo.MetricQueryParam;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.DataSource;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.SqlGenerateUtils;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
        if (Objects.isNull(queryStatement.getQueryParam()) || queryStatement.getIsS2SQL()) {
            return false;
        }
        return !calculateConverterAgg.accept(queryStatement);
    }

    @Override
    public void convert(QueryStatement queryStatement) throws Exception {
        QueryParam queryParam = queryStatement.getQueryParam();
        MetricQueryParam metricQueryParam = queryStatement.getMetricQueryParam();
        MetricQueryParam metricReq = generateSqlCommand(queryStatement.getQueryParam(), queryStatement);
        queryStatement.setMinMaxTime(sqlGenerateUtils.getBeginEndTime(queryParam, null));
        BeanUtils.copyProperties(metricReq, metricQueryParam);
    }

    public MetricQueryParam generateSqlCommand(QueryParam queryParam, QueryStatement queryStatement) {
        MetricQueryParam metricQueryParam = new MetricQueryParam();
        metricQueryParam.setMetrics(queryParam.getMetrics());
        metricQueryParam.setDimensions(queryParam.getGroups());
        String where = sqlGenerateUtils.generateWhere(queryParam, null);
        log.info("in generateSqlCommend, complete where:{}", where);

        metricQueryParam.setWhere(where);
        metricQueryParam.setOrder(queryParam.getOrders().stream()
                .map(order -> new ColumnOrder(order.getColumn(), order.getDirection())).collect(Collectors.toList()));
        metricQueryParam.setVariables(queryParam.getParams().stream()
                .collect(Collectors.toMap(Param::getName, Param::getValue, (k1, k2) -> k1)));
        metricQueryParam.setLimit(queryParam.getLimit());

        // support detail query
        if (queryParam.getQueryType().isNativeAggQuery() && CollectionUtils.isEmpty(metricQueryParam.getMetrics())) {
            Map<Long, DataSource> modelMap = queryStatement.getSemanticModel().getModelMap();
            for (Long modelId : modelMap.keySet()) {
                String modelBizName = modelMap.get(modelId).getName();
                String internalMetricName = sqlGenerateUtils.generateInternalMetricName(modelBizName);
                metricQueryParam.getMetrics().add(internalMetricName);
            }
        }

        return metricQueryParam;
    }

}
