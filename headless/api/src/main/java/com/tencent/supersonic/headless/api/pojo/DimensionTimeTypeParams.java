package com.tencent.supersonic.headless.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DimensionTimeTypeParams implements Serializable {

    private String isPrimary = "true";

    private String timeGranularity = "day";
}
