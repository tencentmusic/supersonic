package com.tencent.supersonic.chat.api.pojo.request;

import com.tencent.supersonic.common.pojo.enums.TaskStatusEnum;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class DictTaskFilterReq {

    private Long id;

    private String name;

    private String createdBy;

    private String createdAt;

    private TaskStatusEnum status;
}