package com.tencent.supersonic.semantic.core.domain.pojo;

import lombok.Data;


@Data
public class MetaFilter {

    private Long id;

    private String name;

    private String bizName;

    private String createdBy;

    private Long domainId;

    private Integer sensitiveLevel;

    private Integer status;

}
