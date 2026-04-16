package com.tencent.supersonic.headless.server.pojo;

import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class TagObjectFilter extends MetaFilter {

    private List<Long> domainIds;
}
