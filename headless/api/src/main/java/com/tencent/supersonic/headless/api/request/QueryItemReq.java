package com.tencent.supersonic.headless.api.request;

import com.tencent.supersonic.common.pojo.DateConf;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class QueryItemReq {

    @NotEmpty(message = "ids不可为空")
    private List<Long> ids;

    private DateConf dateConf = new DateConf();

    //result size of single id
    private Long limit;

}
