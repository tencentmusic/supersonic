package com.tencent.supersonic.headless.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DrillDownDimension {

    private Long dimensionId;

    private boolean necessary;

    private boolean inheritedFromModel;

    public DrillDownDimension(Long dimensionId) {
        this.dimensionId = dimensionId;
    }

    public DrillDownDimension(Long dimensionId, boolean necessary) {
        this.dimensionId = dimensionId;
        this.necessary = necessary;
    }
}
