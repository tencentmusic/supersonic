package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.headless.api.pojo.SchemaItem;
import lombok.Data;

import java.util.List;

@Data
public class ClassReq extends SchemaItem {

    private Long domainId;
    private Long tagObjectId;
    private Long parentId;
    private List<Long> itemIds;
}
