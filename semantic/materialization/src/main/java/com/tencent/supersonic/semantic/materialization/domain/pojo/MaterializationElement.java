package com.tencent.supersonic.semantic.materialization.domain.pojo;


import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.semantic.api.materialization.enums.ElementFrequencyEnum;
import com.tencent.supersonic.semantic.api.materialization.enums.ElementTypeEnum;
import lombok.Data;

@Data
public class MaterializationElement extends RecordInfo {
    private Long id;
    private TypeEnums type;
    private Long materializationId;
    private String depends;
    private ElementTypeEnum elementType;
    private String defaultValue;
    private String outlier;
    private ElementFrequencyEnum frequency;
    private String description;
    private StatusEnum status;
}