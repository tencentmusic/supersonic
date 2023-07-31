package com.tencent.supersonic.semantic.api.model.request;

import com.tencent.supersonic.common.pojo.PageBaseReq;
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
