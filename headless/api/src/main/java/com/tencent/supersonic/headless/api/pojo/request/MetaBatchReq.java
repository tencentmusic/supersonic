package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;
import java.util.List;

@Data
public class MetaBatchReq {

    private List<Long> ids;

    private Integer status;

}
