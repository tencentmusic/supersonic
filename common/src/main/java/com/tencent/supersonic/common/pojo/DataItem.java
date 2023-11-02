package com.tencent.supersonic.common.pojo;

import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataItem {

    private Long id;

    private String bizName;

    private String name;

    private String newName;

    private TypeEnums type;

    private Long modelId;

    public String getNewName() {
        return newName == null ? name : newName;
    }
}
