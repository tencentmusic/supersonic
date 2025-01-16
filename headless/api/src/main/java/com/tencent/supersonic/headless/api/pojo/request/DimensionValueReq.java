package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.DateConf;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

@Data
public class DimensionValueReq {

    private Integer agentId;

    @NotNull
    private Long elementID;

    private Long modelId;

    private String bizName;

    @NotNull
    private String value;

    private Set<Long> dataSetIds;

    private DateConf dateInfo = new DateConf();

    private String dimensionBizName;

    public String getBizName() {
        return StringUtils.isBlank(bizName) ? dimensionBizName : bizName;
    }
}
