package com.tencent.supersonic.common.pojo;

import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataItem {

    private String id;

    private String bizName;

    private String name;

    private String newName;

    private TypeEnums type;

    private String modelId;

    private String domainId;

    private String defaultAgg;

    public String getNewName() {
        return newName == null ? name : newName;
    }
}
