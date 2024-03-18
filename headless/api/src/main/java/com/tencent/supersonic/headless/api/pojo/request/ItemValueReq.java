package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Data
@ToString
public class ItemValueReq {

    private SchemaElementType type;

    @NotNull
    private Long itemId;

    private DateConf dateConf;

    private Long limit = 10L;
}