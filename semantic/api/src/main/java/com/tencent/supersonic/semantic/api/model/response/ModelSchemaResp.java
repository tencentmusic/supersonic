package com.tencent.supersonic.semantic.api.model.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelSchemaResp extends ModelResp {

    private List<MetricSchemaResp> metrics;
    private List<DimSchemaResp> dimensions;

}