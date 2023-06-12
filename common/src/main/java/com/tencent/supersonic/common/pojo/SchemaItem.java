package com.tencent.supersonic.common.pojo;

import com.tencent.supersonic.common.enums.SensitiveLevelEnum;
import com.tencent.supersonic.common.enums.StatusEnum;
import com.tencent.supersonic.common.enums.TypeEnums;
import com.tencent.supersonic.common.util.RecordInfo;
import lombok.Data;

@Data
public class SchemaItem extends RecordInfo {

    private Long id;

    private String name;

    private String bizName;

    private String description;

    private Integer status = StatusEnum.ONLINE.getCode();

    private TypeEnums typeEnum;

    private Integer sensitiveLevel = SensitiveLevelEnum.LOW.getCode();


}
