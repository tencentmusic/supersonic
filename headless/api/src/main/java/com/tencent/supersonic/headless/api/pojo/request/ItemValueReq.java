package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.DateConf;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ItemValueReq {

    @NotNull
    private Long id;

    private DateConf dateConf;

    private Long limit = 10L;
}
