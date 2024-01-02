package com.tencent.supersonic.headless.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class DimensionTimeTypeParams {

    private String isPrimary = "true";

    private String timeGranularity = "day";

}
