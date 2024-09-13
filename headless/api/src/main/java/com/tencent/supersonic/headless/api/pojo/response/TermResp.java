package com.tencent.supersonic.headless.api.pojo.response;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TermResp extends RecordInfo {

    private Long id;

    private Long domainId;

    private String name;

    private String description;

    private List<String> alias = Lists.newArrayList();

    private List<Long> relatedMetrics = Lists.newArrayList();

    private List<Long> relateDimensions = Lists.newArrayList();
}
