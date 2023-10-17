package com.tencent.supersonic.semantic.api.materialization.pojo;

import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MaterializationConfFilter extends MaterializationFilter {

    private Long id;
    private Boolean containElements = false;

    private TypeEnums type;
    private Long materializationId;
    private Long elementId;
}