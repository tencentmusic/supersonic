package com.tencent.supersonic.headless.core.pojo;

import com.tencent.supersonic.headless.api.pojo.Identify;
import com.tencent.supersonic.headless.api.pojo.Measure;
import com.tencent.supersonic.headless.api.pojo.response.DimSchemaResp;
import com.tencent.supersonic.headless.core.translator.parser.s2sql.Materialization;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DataModel {

    private Long id;

    private String name;

    private Long modelId;

    private String type;

    private String sqlQuery;

    private String tableQuery;

    private List<Identify> identifiers;

    private List<DimSchemaResp> dimensions;

    private List<Measure> measures;

    private String aggTime;

    private com.tencent.supersonic.headless.core.translator.parser.s2sql.Materialization.TimePartType timePartType =
            Materialization.TimePartType.None;
}
