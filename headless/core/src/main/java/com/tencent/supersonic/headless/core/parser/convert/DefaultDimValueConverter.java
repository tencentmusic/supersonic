package com.tencent.supersonic.headless.core.parser.convert;

import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.common.core.request.QueryStructReq;
import com.tencent.supersonic.headless.common.server.response.DimensionResp;
import com.tencent.supersonic.headless.core.parser.HeadlessConverter;
import com.tencent.supersonic.headless.core.persistence.pojo.QueryStatement;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.Catalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component("DefaultDimValueConverter")
public class DefaultDimValueConverter implements HeadlessConverter {

    @Override
    public boolean accept(QueryStatement queryStatement) {
        if (Objects.isNull(queryStatement.getQueryStructReq()) || queryStatement.getIsS2SQL()) {
            return false;
        }
        return true;
    }

    @Override
    public void converter(Catalog catalog, QueryStatement queryStatement) {
        QueryStructReq queryStructCmd = queryStatement.getQueryStructReq();
        MetaFilter metaFilter = new MetaFilter(queryStructCmd.getModelIds());
        List<DimensionResp> dimensionResps = catalog.getDimensions(metaFilter);
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
