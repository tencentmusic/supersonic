package com.tencent.supersonic.headless.common.model.pojo;

import com.google.common.collect.Lists;
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

    List<DrillDownDimension> drillDownDimensions = Lists.newArrayList();

}
