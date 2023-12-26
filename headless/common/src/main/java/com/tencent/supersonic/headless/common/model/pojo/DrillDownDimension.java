package com.tencent.supersonic.headless.common.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DrillDownDimension {

    private Long dimensionId;

    private boolean necessary;

    public DrillDownDimension(Long dimensionId) {
        this.dimensionId = dimensionId;
    }
}