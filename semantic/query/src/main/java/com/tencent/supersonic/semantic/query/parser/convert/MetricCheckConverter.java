package com.tencent.supersonic.semantic.query.parser.convert;

import com.tencent.supersonic.common.pojo.exception.InvalidArgumentException;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.query.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.query.parser.SemanticConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Component("MetricCheckConverter")
@Slf4j
public class MetricCheckConverter implements SemanticConverter {

    @Override
    public boolean accept(QueryStructReq queryStructCmd) {
        if (queryStructCmd.getNativeQuery()) {
            return false;
        }
        return !CollectionUtils.isEmpty(queryStructCmd.getAggregators());
    }

    @Override
    public void converter(Catalog catalog, QueryStructReq queryStructReq, ParseSqlReq sqlCommend,
            MetricReq metricCommand) throws Exception {
        Long modelId = queryStructReq.getModelId();
        List<MetricResp> metricResps = catalog.getMetrics(modelId);
        List<DimensionResp> dimensionResps = catalog.getDimensions(modelId);
        Map<Long, DimensionResp> dimensionMap = dimensionResps.stream()
                .collect(Collectors.toMap(DimensionResp::getId, d -> d));
        List<String> metricBizNames = queryStructReq.getMetrics();
        List<String> dimensionFilterBizNames = queryStructReq.getDimensionFilters().stream()
                .map(Filter::getBizName).collect(Collectors.toList());
        List<MetricResp> metricToQuery = metricResps.stream().filter(metricResp ->
                        metricBizNames.contains(metricResp.getBizName())).collect(Collectors.toList());
        List<Long> dimensionToFilter = dimensionResps.stream().filter(dimensionResp ->
                dimensionFilterBizNames.contains(dimensionResp.getBizName()))
                .map(DimensionResp::getId).collect(Collectors.toList());
        for (MetricResp metricResp : metricToQuery) {
            Set<Long> necessaryDimensionIds = metricResp.getNecessaryDimensionIds();
            if (CollectionUtils.isEmpty(necessaryDimensionIds)) {
                continue;
            }
            DimensionResp dimensionResp = null;
            for (Long dimensionId : necessaryDimensionIds) {
                dimensionResp = dimensionMap.get(dimensionId);
                if (dimensionResp != null) {
                    break;
                }
            }
            if (dimensionResp == null) {
                continue;
            }
            String message = String.format("该指标必须配合维度[%s]来进行过滤查询", dimensionResp.getName());
            if (CollectionUtils.isEmpty(dimensionToFilter)) {
                throw new InvalidArgumentException(message);
            }
            boolean flag = false;
            for (Long dimensionId : dimensionToFilter) {
                if (necessaryDimensionIds.contains(dimensionId)) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                throw new InvalidArgumentException(message);
            }
        }
    }
}
