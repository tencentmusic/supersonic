package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;

@Data
public class FileReq {
    private String taskId;
    private String serviceName;
    private String serviceType;
}
