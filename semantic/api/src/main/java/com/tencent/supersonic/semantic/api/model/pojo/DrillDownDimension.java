package com.tencent.supersonic.semantic.api.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DrillDownDimension {

    private Long dimensionId;

    private boolean necessary;

}