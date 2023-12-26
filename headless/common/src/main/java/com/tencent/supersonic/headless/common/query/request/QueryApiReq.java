package com.tencent.supersonic.headless.common.query.request;

import com.tencent.supersonic.common.pojo.DateConf;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class QueryApiReq {

    @NotEmpty(message = "ids不可为空")
    private List<Long> ids;

    private DateConf dateConf = new DateConf();

}
