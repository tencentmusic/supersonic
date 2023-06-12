package com.tencent.supersonic.semantic.api.core.request;

import com.tencent.supersonic.common.request.PageBaseReq;
import lombok.Data;

@Data
public class PageSchemaItemReq extends PageBaseReq {

    private Long id;
    private String name;
    private String bizName;
    private String createdBy;
    private Long domainId;
    private Integer sensitiveLevel;
    private Integer status;
}
