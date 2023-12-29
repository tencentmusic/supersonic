package com.tencent.supersonic.headless.materialization.domain.pojo;


import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.headless.common.model.enums.ModelSourceTypeEnum;
import com.tencent.supersonic.headless.common.materialization.enums.UpdateCycleEnum;
import lombok.Data;

import java.util.List;

@Data
public class Materialization extends RecordInfo {

    private Long id;
    private String name;
    private ModelSourceTypeEnum materializedType;
    private UpdateCycleEnum updateCycle;
    private Long modelId;
    private Long databaseId;
    private Integer level;
    private String destinationTable;
    private String dateInfo;
    private String entities;
    private List<String> principals;
    private String description;
    private StatusEnum status = StatusEnum.ONLINE;
}