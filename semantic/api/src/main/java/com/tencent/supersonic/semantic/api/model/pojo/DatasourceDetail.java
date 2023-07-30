package com.tencent.supersonic.semantic.api.model.pojo;

import java.util.List;
import lombok.Data;


@Data
public class DatasourceDetail {

    private String queryType;

    private String sqlQuery;

    private String tableQuery;

    private List<Identify> identifiers;

    private List<Dim> dimensions;

    private List<Measure> measures;


}
