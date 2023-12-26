package com.tencent.supersonic.headless.common.materialization.request;


import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.common.materialization.enums.ElementFrequencyEnum;
import com.tencent.supersonic.headless.common.materialization.enums.ElementTypeEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MaterializationElementReq extends RecordInfo {
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