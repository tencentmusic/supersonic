package com.tencent.supersonic.semantic.api.materialization.response;


import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import lombok.Data;

@Data
public class MaterializationRecordResp extends RecordInfo {

    private Long id;
    private Long materializationId;
    private TypeEnums elementType;
    private Long elementId;
    private String elementName;
    private String dataTime;
    private TaskStatusEnum taskStatus;
    private String taskId;
    private Integer retryCount;
    private Long sourceCount;
    private Long sinkCount;
    private String message;
}