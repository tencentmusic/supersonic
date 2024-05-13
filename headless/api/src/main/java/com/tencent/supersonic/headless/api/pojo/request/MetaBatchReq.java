package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.enums.EventType;
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

    /**
     * 批量执行分类信息
     */
    private EventType type;
    private List<String> classifications;

}
