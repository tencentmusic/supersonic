package com.tencent.supersonic.semantic.api.model.request;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.PageBaseReq;
import lombok.Data;
import java.util.List;

@Data
public class PageSchemaItemReq extends PageBaseReq {

    private Long id;
    private String name;
    private String bizName;
    private String createdBy;
    private List<Long> domainIds = Lists.newArrayList();
    private List<Long> modelIds = Lists.newArrayList();
    private Integer sensitiveLevel;
    private Integer status;
}
