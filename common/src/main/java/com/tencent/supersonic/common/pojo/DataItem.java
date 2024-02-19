package com.tencent.supersonic.common.pojo;

import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DataItem {

    /***
     * This field uses an underscore (_) at the end.
     */
    private String id;

    private String bizName;

    private String name;

    private String newName;

    private TypeEnums type;

    /***
     * This field uses an underscore (_) at the end.
     */
    private String modelId;

    private String defaultAgg;

    public String getNewName() {
        return newName == null ? name : newName;
    }
}
