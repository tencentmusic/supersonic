package com.tencent.supersonic.headless.api.pojo.request;

import javax.validation.constraints.NotNull;

import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import lombok.Data;

@Data
public class TagReq extends RecordInfo {

    private Long id;

    @NotNull
    private TagDefineType tagDefineType;

    @NotNull
    private Long itemId;
}
