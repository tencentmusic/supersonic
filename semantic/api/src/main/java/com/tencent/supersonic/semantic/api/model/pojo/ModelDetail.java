package com.tencent.supersonic.semantic.api.model.pojo;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;


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

}
