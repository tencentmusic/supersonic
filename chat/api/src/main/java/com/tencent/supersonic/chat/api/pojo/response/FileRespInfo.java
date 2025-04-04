package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

@Data
public class FileRespInfo {
    // 解析文档的fileId
    private String fileId;

    // 文档解析后的内容，解析成功时携带
    private String fileContent;

    // 文档解析后的内容字节数大小，解析成功时携带
    private Integer fileParseSize;

    // 文档解析后内容安审是否通过，0代表通过，1代表不通过，默认为0
    private Integer fileSecure;

    // 可用内容字节数占比，文档解析后的内容长度超过限定值后携带
    private String fileSizePercent;

    // 任务ID
    private String taskId;
}
