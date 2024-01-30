package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.DateConf;
import lombok.Data;

import javax.validation.constraints.Size;
import java.util.List;

@Data
public class QueryItemReq {

    @Size(max = 5, min = 1)
    private List<Long> ids;

    private DateConf dateConf = new DateConf();

    //result size of single id
    private Long limit = 1000L;

    public Long getLimit() {
        if (limit > 10000) {
            return 10000L;
        }
        return limit;
    }

}
