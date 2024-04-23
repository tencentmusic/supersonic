package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.Date;

@Data
public class ClassResp {

    private Long id;

    private Long domainId;
    private String domainName;
    private Long tagObjectId;
    private String tagObjectName;

    private String name;
    private String bizName;
    private String description;

    private String fullPath;

    /**
     * 分类状态
     */
    private Integer status;

    /**
     * METRIC、DIMENSION、TAG
     */
    private String type;
    private String itemIds;

    private Date createdAt;
    private String createdBy;
    private Date updatedAt;
    private String updatedBy;

    private boolean hasEditPermission = false;

}