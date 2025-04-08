package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

@Data
public class FileUpLoadResponse {
    /**
     * 资源ID
     */
    private String fileId;

    /**
     * 资源文件类型: VIDEO - 视频素材 IMAGE - 图片素材 AUDIO - 音频素材 TEXT - 文本文件 等等
     */
    private String fileType;

    /**
     * 视频帧速率
     */
    private String frameRate;

    /**
     * 文件路径
     */
    private String filePath;
}
