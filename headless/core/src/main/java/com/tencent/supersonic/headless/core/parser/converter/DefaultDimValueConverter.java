package com.tencent.supersonic.headless.core.parser.converter;

import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.request.QueryStructReq;
import com.tencent.supersonic.headless.core.parser.HeadlessConverter;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.Dimension;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
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
    public void convert(QueryStatement queryStatement) {
        QueryStructReq queryStructCmd = queryStatement.getQueryStructReq();
        List<Dimension> dimensions = queryStatement.getSemanticModel().getDimensions().stream()
                .filter(dimension -> !CollectionUtils.isEmpty(dimension.getDefaultValues()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(dimensions)) {
            return;
        }
        log.info("dimension with default values:{}, queryStruct:{}", dimensions, queryStructCmd);
        //add dimension default value to filter
        List<String> dimensionFilterBizName = queryStructCmd.getDimensionFilters().stream()
                .map(Filter::getBizName).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(dimensionFilterBizName)) {
            return;
        }
        for (Dimension dimensionResp : dimensions) {
            Filter filter = new Filter();
            filter.setBizName(dimensionResp.getBizName());
            filter.setValue(dimensionResp.getDefaultValues());
            filter.setOperator(FilterOperatorEnum.IN);
            filter.setName(dimensionResp.getName());
            queryStructCmd.getDimensionFilters().add(filter);
        }
    }

}