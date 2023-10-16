package com.tencent.supersonic.semantic.api.materialization.response;

import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.semantic.api.materialization.enums.MaterializedTypeEnum;
import com.tencent.supersonic.semantic.api.materialization.enums.UpdateCycleEnum;
import lombok.Data;

import java.util.List;

@Data
public class MaterializationResp extends RecordInfo {
    private Long id;
    private String name;
    private MaterializedTypeEnum materializedType;
    private UpdateCycleEnum updateCycle;
    private Long modelId;
    private Long databaseId;
    private Integer level;
    private String destinationTable;
    private String dateInfo;
    private String entities;
    private List<String> principals;
    private String description;
    private List<MaterializationElementResp> materializationElementRespList;
}