package com.tencent.supersonic.semantic.query.parser.convert;

import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.semantic.api.query.pojo.Param;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.query.parser.SemanticConverter;
import com.tencent.supersonic.semantic.query.utils.QueryStructUtils;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component("ParserDefaultConverter")
@Slf4j
public class ParserDefaultConverter implements SemanticConverter {


    private final CalculateAggConverter calculateCoverterAgg;
    private final QueryStructUtils queryStructUtils;

    public ParserDefaultConverter(
            CalculateAggConverter calculateCoverterAgg,
            QueryStructUtils queryStructUtils) {
        this.calculateCoverterAgg = calculateCoverterAgg;
        this.queryStructUtils = queryStructUtils;
    }

    @Override
    public boolean accept(QueryStructReq queryStructCmd) {
        return !calculateCoverterAgg.accept(queryStructCmd);
    }

    @Override
    public void converter(Catalog catalog, QueryStructReq queryStructCmd, ParseSqlReq sqlCommend,
            MetricReq metricCommand)
            throws Exception {
        MetricReq metricReq = generateSqlCommand(catalog, queryStructCmd);
        BeanUtils.copyProperties(metricReq, metricCommand);
    }

    public MetricReq generateSqlCommand(Catalog catalog, QueryStructReq queryStructCmd) {
        MetricReq sqlCommend = new MetricReq();
        sqlCommend.setMetrics(queryStructCmd.getMetrics());
        sqlCommend.setDimensions(queryStructCmd.getGroups());
        String where = queryStructUtils.generateWhere(queryStructCmd);
        log.info("in generateSqlCommend, complete where:{}", where);

        sqlCommend.setWhere(where);
        sqlCommend.setOrder(queryStructCmd.getOrders().stream()
                .map(order -> new ColumnOrder(order.getColumn(), order.getDirection())).collect(Collectors.toList()));
        sqlCommend.setVariables(queryStructCmd.getParams().stream()
                .collect(Collectors.toMap(Param::getName, Param::getValue, (k1, k2) -> k1)));
        sqlCommend.setLimit(queryStructCmd.getLimit());
        String rootPath = catalog.getModelFullPath(queryStructCmd.getModelIds());
        sqlCommend.setRootPath(rootPath);

        // todo tmp delete
        // support detail query
        if (queryStructCmd.getQueryType().isNativeAggQuery() && CollectionUtils.isEmpty(sqlCommend.getMetrics())) {
            for (Long modelId : queryStructCmd.getModelIds()) {
                String internalMetricName = queryStructUtils.generateInternalMetricName(
                        modelId, queryStructCmd.getGroups());
                sqlCommend.getMetrics().add(internalMetricName);
            }
        }

        return sqlCommend;
    }

}
