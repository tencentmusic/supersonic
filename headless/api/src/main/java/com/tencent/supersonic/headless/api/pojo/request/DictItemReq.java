package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.api.pojo.ItemValueConfig;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class DictItemReq {

    private Long id;
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
}