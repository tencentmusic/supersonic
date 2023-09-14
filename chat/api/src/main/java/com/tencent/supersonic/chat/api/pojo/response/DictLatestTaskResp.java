package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

@ToString
@Data
public class DictLatestTaskResp {

    private Long dimId;

    private Long id;

    private String name;

    private String description;

    private String command;

    private TaskStatusEnum status;

    private String createdBy;

    private Date createdAt;

    private Long elapsedMs;
}