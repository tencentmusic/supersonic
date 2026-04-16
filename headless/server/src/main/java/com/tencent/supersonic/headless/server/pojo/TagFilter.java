package com.tencent.supersonic.headless.server.pojo;

import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class TagFilter extends MetaFilter {

    private List<Long> itemIds;
    private TagDefineType tagDefineType;
    private boolean hasCollect;
}
