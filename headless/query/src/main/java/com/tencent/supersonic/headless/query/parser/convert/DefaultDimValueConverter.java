package com.tencent.supersonic.headless.query.parser.convert;

import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.model.response.DimensionResp;
import com.tencent.supersonic.headless.api.query.request.QueryStructReq;
import com.tencent.supersonic.headless.model.domain.Catalog;
import com.tencent.supersonic.headless.query.parser.HeadlessConverter;
import com.tencent.supersonic.headless.query.persistence.pojo.QueryStatement;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
    public void converter(Catalog catalog, QueryStatement queryStatement) throws Exception {
        QueryStructReq queryStructCmd = queryStatement.getQueryStructReq();
        List<DimensionResp> dimensionResps = catalog.getDimensions(queryStructCmd.getModelIds());
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
