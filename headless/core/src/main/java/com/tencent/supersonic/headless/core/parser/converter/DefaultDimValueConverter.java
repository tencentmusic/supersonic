package com.tencent.supersonic.headless.core.parser.converter;

import com.tencent.supersonic.common.pojo.Filter;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.core.parser.calcite.s2sql.Dimension;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
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
        if (Objects.isNull(queryStatement.getQueryParam()) || queryStatement.getIsS2SQL()) {
            return false;
        }
        return true;
    }

    @Override
    public void convert(QueryStatement queryStatement) {
        QueryParam queryParam = queryStatement.getQueryParam();
        List<Dimension> dimensions = queryStatement.getSemanticModel().getDimensions().stream()
                .filter(dimension -> !CollectionUtils.isEmpty(dimension.getDefaultValues()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(dimensions)) {
            return;
        }
        log.info("dimension with default values:{}, queryStruct:{}", dimensions, queryParam);
        //add dimension default value to filter
        List<String> dimensionFilterBizName = queryParam.getDimensionFilters().stream()
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
            queryParam.getDimensionFilters().add(filter);
        }
    }

}