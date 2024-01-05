package com.tencent.supersonic.chat.core.knowledge;


import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;

import java.util.Date;
import java.util.Set;

import lombok.Data;

@Data
public class DimValueDictInfo {

    private Long id;

    private String name;

    private String description;

    private String command;

    private TaskStatusEnum status;

    private String createdBy;

    private Date createdAt;

    private Long elapsedMs;

    private Set<Long> dimIds;
}