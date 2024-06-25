package com.tencent.supersonic.headless.server.pojo;

import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import lombok.Data;

import java.util.List;

@Data
public class TagFilter extends MetaFilter {

    private List<Long> itemIds;
    private TagDefineType tagDefineType;
    private boolean hasCollect;
}
