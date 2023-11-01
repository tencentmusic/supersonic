package com.tencent.supersonic.semantic.query.parser.convert;

import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.api.query.request.ParseSqlReq;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.query.parser.SemanticConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component("DefaultDimValueConverter")
public class DefaultDimValueConverter implements SemanticConverter {

    @Override
    public boolean accept(QueryStructReq queryStructCmd) {
        return true;
    }

    @Override
    public void converter(Catalog catalog, QueryStructReq queryStructCmd,
                          ParseSqlReq sqlCommend, MetricReq metricCommand) throws Exception {
        List<DimensionResp> dimensionResps = catalog.getDimensions(queryStructCmd.getModelId());
        //dimension which has default values
        dimensionResps = dimensionResps.stream()
                .filter(dimensionResp -> !CollectionUtils.isEmpty(dimensionResp.getDefaultValues()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(dimensionResps)) {
            return;
        }
        log.info("dimension with default values:{}, queryStruct:{}", dimensionResps, queryStructCmd);
        //add dimension default value to filter
        List<String> dimensionFilterBizName = queryStructCmd.getDimensionFilters().stream()
                .map(Filter::getBizName).collect(Collectors.toList());
        for (DimensionResp dimensionResp : dimensionResps) {
            if (!dimensionFilterBizName.contains(dimensionResp.getBizName())) {
                Filter filter = new Filter();
                filter.setBizName(dimensionResp.getBizName());
                filter.setValue(dimensionResp.getDefaultValues());
                filter.setOperator(FilterOperatorEnum.IN);
                filter.setName(dimensionResp.getName());
                queryStructCmd.getDimensionFilters().add(filter);
            }
        }
    }

}
