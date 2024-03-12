package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;
import java.util.List;

@Data
public class MetaBatchReq {

    private List<Long> ids;

    private List<String> bizNames;

    private List<Long> modelIds;

    /**
     * 最后变更的状态
     */
    private Integer status;

}
