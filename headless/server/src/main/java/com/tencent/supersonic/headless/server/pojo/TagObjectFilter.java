package com.tencent.supersonic.headless.server.pojo;

import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import lombok.Data;

import java.util.List;

@Data
public class TagObjectFilter extends MetaFilter {

    private List<Long> domainIds;
}
