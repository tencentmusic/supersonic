package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class TagResp {

    private Long id;

    private Long domainId;

    private String domainName;

    private Long modelId;

    private String modelName;

    private Long tagObjectId;

    private String tagObjectName;

    private Boolean isCollect;

    private boolean hasAdminRes;

    private String tagDefineType;

    private Long itemId;

    private String name;

    private String bizName;

    private String description;

}
