package com.tencent.supersonic.headless.api.pojo;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.enums.ApiItemType;
import lombok.Data;
import java.util.List;

@Data
public class Item {

    private Long id;

    private String name;

    private String bizName;

    private String createdBy;

    private ApiItemType type;

    private List<Item> relateItems = Lists.newArrayList();

    public String getValue() {
        return name;
    }

}
