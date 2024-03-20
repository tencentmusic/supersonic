package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.headless.api.pojo.enums.TagDefineType;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class TagReq extends RecordInfo {

    private Long id;

    @NotNull
    private TagDefineType tagDefineType;

    @NotNull
    private Long itemId;

}
