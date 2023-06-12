package com.tencent.supersonic.knowledge.domain.pojo;


import com.tencent.supersonic.common.enums.TaskStatusEnum;
import java.util.Date;
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
}