package com.tencent.supersonic.semantic.api.materialization.pojo;


import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import java.util.List;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class MaterializationRecordFilter {

    private Long id;
    private Long materializationId;
    private TypeEnums elementType;
    private Long elementId;
    private String elementName;
    private List<TaskStatusEnum> taskStatus;
    private String taskId;
    private String createdBy;
    private Date createdAt;
    private String startDataTime;
    private String endDataTime;
    private List<Long> materializationIds;
}