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

    private List<Field> fields;

    public String getSqlQuery() {
        if (StringUtils.isNotBlank(sqlQuery) && sqlQuery.endsWith(";")) {
            sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 1);
        }
        return sqlQuery;
    }

    public List<Dim> filterTimeDims() {
        if (CollectionUtils.isEmpty(dimensions)) {
            return Lists.newArrayList();
        }
        return dimensions.stream()
                .filter(dim -> DimensionType.time.name().equalsIgnoreCase(dim.getType()))
                .collect(Collectors.toList());
    }

    public List<Field> getFields() {
        if (!CollectionUtils.isEmpty(fields)) {
            return fields;
        }
        List<Field> fieldList = Lists.newArrayList();
        //Compatible with older versions
        if (!CollectionUtils.isEmpty(identifiers)) {
            fieldList.addAll(identifiers.stream()
                    .map(identify -> Field.builder().fieldName(identify.getFieldName()).build())
                    .collect(Collectors.toSet()));
        }
        if (!CollectionUtils.isEmpty(dimensions)) {
            fieldList.addAll(dimensions.stream()
                    .map(dim -> Field.builder().fieldName(dim.getFieldName()).build())
                    .collect(Collectors.toSet()));
        }
        if (!CollectionUtils.isEmpty(measures)) {
            fieldList.addAll(measures.stream()
                    .map(measure -> Field.builder().fieldName(measure.getFieldName()).build())
                    .collect(Collectors.toSet()));
        }
        return fieldList;
    }

}
