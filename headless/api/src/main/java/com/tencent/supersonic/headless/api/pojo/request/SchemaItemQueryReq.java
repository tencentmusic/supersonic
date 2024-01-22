package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import lombok.Data;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class SchemaItemQueryReq {

    @NotEmpty(message = "id个数不可为空")
    private List<Long> ids;

    //METRIC, DIMENSION
    @NotNull(message = "type不可为空")
    private TypeEnums type;

}
