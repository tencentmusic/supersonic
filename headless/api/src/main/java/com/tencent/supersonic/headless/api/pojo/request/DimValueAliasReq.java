package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.headless.api.pojo.DimValueMap;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author: kanedai
 * @date: 2024/10/31
 */
@Data
public class DimValueAliasReq {

    @NotNull
    private Long id;

    /**
     * alias为空代表删除 否则更新
     */
    DimValueMap dimValueMaps;
}
