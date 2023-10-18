package com.tencent.supersonic.semantic.api.materialization.response;

import com.tencent.supersonic.semantic.api.materialization.enums.MaterializedTypeEnum;
import com.tencent.supersonic.semantic.api.materialization.enums.UpdateCycleEnum;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterializationSourceResp {

    private Long materializationId;
    private Long dataSourceId;
    private Long modelId;
    private String sql;
    private List<String> fields;
    private String sourceDb;
    private String sourceTable;

    private String dateInfo;
    private String entities;
    private MaterializedTypeEnum materializedType;
    private UpdateCycleEnum updateCycle;
    private DatabaseResp databaseResp;
    private String depends;
    private Map<Long, String> dimensions;
    private Map<Long, String> metrics;
}
