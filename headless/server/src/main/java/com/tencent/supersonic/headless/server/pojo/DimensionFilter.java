package com.tencent.supersonic.headless.server.pojo;

import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class DimensionFilter extends MetaFilter {

    private Integer isTag;
}
