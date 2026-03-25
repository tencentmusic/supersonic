package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.headless.api.pojo.DimValueMap;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author: kanedai
 * @date: 2024/10/31
 */
@Data
public class DimValueAliasReq {

    @NotNull
    private Long id;

    /**
     * alias 为空代表删除 否则更新
     */
    private List<DimValueMap> dimValueMaps;
}
