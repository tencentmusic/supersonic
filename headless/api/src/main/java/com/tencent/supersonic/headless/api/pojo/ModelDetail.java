package com.tencent.supersonic.headless.api.pojo;

import com.google.common.collect.Lists;
import com.tencent.supersonic.headless.api.enums.DimensionType;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;


@Data
public class ModelDetail {

    private String queryType;

    private String sqlQuery;

    private String tableQuery;

    private List<Identify> identifiers;

    private List<Dim> dimensions;

    private List<Measure> measures;

    public String getSqlQuery() {
        if (StringUtils.isNotBlank(sqlQuery) && sqlQuery.endsWith(";")) {
            sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 1);
        }
        return sqlQuery;
    }

    public List<Dim> getTimeDims() {
        if (CollectionUtils.isEmpty(dimensions)) {
            return Lists.newArrayList();
        }
        return dimensions.stream()
                .filter(dim -> DimensionType.time.name().equalsIgnoreCase(dim.getType()))
                .collect(Collectors.toList());
    }

}
