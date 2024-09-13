package com.tencent.supersonic.headless.api.pojo.response;

import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.Data;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString(callSuper = true)
public class TagResp extends RecordInfo {

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

    private Integer sensitiveLevel;

    private Map<String, Object> ext = new HashMap();
}
