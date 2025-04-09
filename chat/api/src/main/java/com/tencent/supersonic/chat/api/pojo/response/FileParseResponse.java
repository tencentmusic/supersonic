package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

import java.util.List;

@Data
public class FileParseResponse {
    /**
     * 处理结果状态： RUNNING - 任务处理中 COMPLETED - 任务处理成功 ERROR - 任务处理失败 NONE - 无该任务信息
     */
    private String status;

    /**
     * 文档解析任务下发后的结果
     */
    private List<FileTaskRespInfo> resultList;

    /**
     * 状态信息
     */
    private String msg;

    /**
     * 提示词
     */
    private List<String> prompt;
}
