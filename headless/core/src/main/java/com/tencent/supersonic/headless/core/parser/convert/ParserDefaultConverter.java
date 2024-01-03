package com.tencent.supersonic.headless.core.parser.convert;

import com.tencent.supersonic.common.pojo.ColumnOrder;
import com.tencent.supersonic.headless.api.pojo.Param;
import com.tencent.supersonic.headless.api.request.MetricQueryReq;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.core.parser.HeadlessConverter;
import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.QueryStructUtils;
import com.tencent.supersonic.headless.server.service.Catalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.stream.Collectors;

@Component("ParserDefaultConverter")
@Slf4j
public class ParserDefaultConverter implements HeadlessConverter {


    private final CalculateAggConverter calculateCoverterAgg;
    private final QueryStructUtils queryStructUtils;

    public ParserDefaultConverter(
            CalculateAggConverter calculateCoverterAgg,
            QueryStructUtils queryStructUtils) {
        this.calculateCoverterAgg = calculateCoverterAgg;
        this.queryStructUtils = queryStructUtils;
    }

    @Override
    public boolean accept(QueryStatement queryStatement) {
        if (Objects.isNull(queryStatement.getQueryStructReq()) || queryStatement.getIsS2SQL()) {
            return false;
        }
        return !calculateCoverterAgg.accept(queryStatement);
    }

    @Override
    public void converter(Catalog catalog, QueryStatement queryStatement) throws Exception {
        QueryStructReq queryStructCmd = queryStatement.getQueryStructReq();
        MetricQueryReq metricCommand = queryStatement.getMetricReq();
        MetricQueryReq metricReq = generateSqlCommand(catalog, queryStructCmd);
        queryStatement.setMinMaxTime(queryStructUtils.getBeginEndTime(queryStructCmd));
        BeanUtils.copyProperties(metricReq, metricCommand);
    }

    public MetricQueryReq generateSqlCommand(Catalog catalog, QueryStructReq queryStructCmd) {
        MetricQueryReq sqlCommend = new MetricQueryReq();
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
