package com.tencent.supersonic.semantic.api.core.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DomainSchemaResp extends DomainResp {

    private List<String> entityNames;
    private List<MetricSchemaResp> metrics;
    private List<DimSchemaResp> dimensions;

}