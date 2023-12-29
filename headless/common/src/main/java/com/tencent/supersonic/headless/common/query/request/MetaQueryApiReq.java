package com.tencent.supersonic.headless.common.query.request;

import com.tencent.supersonic.common.pojo.enums.ApiItemType;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class MetaQueryApiReq {

    @NotEmpty(message = "ids不可为空")
    private List<Long> ids;

    private ApiItemType type = ApiItemType.METRIC;

}
