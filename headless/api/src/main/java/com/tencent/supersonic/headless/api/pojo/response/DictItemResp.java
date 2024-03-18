package com.tencent.supersonic.headless.api.pojo.response;

import static com.tencent.supersonic.common.pojo.Constants.UNDERLINE;

import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.api.pojo.ItemValueConfig;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DictItemResp {

    private Long id;

    private Long modelId;

    private String bizName;

    @NotNull
    private TypeEnums type;
    @NotNull
    private Long itemId;
    private ItemValueConfig config;

    /**
     * ONLINE  - 正常更新
     * OFFLINE - 停止更新,但字典文件不删除
     * DELETED - 停止更新,且删除字典文件
     */
    @NotNull
    private StatusEnum status;

    public String getNature() {
        return UNDERLINE + modelId + UNDERLINE + itemId;
    }

    public String fetchDictFileName() {
        return String.format("dic_value_%d_%s_%s", modelId, type.name(), itemId);
    }

}