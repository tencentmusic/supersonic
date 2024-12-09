package com.tencent.supersonic.headless.api.pojo.request;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.RecordInfo;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TermReq extends RecordInfo {

    private Long id;

    @NotNull(message = "主题域ID不可为空")
    private Long domainId;

    private String name;

    private String description;

    private List<String> alias = Lists.newArrayList();

    private List<Long> relatedMetrics = Lists.newArrayList();

    private List<Long> relateDimensions = Lists.newArrayList();
}
