package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ClassResp {

    private Long id;

    private Long domainId;
    private String domainName;
    private Long dataSetId;

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
    private List<Long> itemIds;

    private Date createdAt;
    private String createdBy;
    private Date updatedAt;
    private String updatedBy;

}