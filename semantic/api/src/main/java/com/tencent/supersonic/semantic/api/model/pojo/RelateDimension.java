package com.tencent.supersonic.semantic.api.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelateDimension {

    List<DrillDownDimension> drillDownDimensions;

}
