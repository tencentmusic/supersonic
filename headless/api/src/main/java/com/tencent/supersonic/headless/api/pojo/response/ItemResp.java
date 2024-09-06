package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemResp {

    private Long id;

    private Long parentId;

    private String name;

    private TypeEnums type;

    private List<ItemResp> children = Lists.newArrayList();

    public ItemResp(Long id, Long parentId, String name, TypeEnums type) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.type = type;
    }

    public boolean isRoot() {
        return parentId == null || parentId == 0;
    }
}
