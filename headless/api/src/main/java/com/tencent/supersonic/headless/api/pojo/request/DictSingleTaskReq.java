package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DictSingleTaskReq {
    @NotNull
    private TypeEnums type;
    @NotNull
    private Long itemId;
}
