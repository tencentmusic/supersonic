package com.tencent.supersonic.headless.api.pojo.response;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class DictTaskResp extends DictItemResp {

    private String name;
    private String description;
    private String taskStatus;
    private Date createdAt;
    private String createdBy;
    private Long elapsedMs;
}
