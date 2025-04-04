package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

@Data
public class FileTaskRespInfo {
    /**
     * 解析文档的fileId
     */
    private String fileId;

    /**
     * 任务ID
     */
    private String taskId;
}
