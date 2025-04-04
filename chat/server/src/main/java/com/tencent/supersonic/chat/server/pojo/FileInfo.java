package com.tencent.supersonic.chat.server.pojo;

import lombok.Data;

@Data
public class FileInfo {
    private String fileId;
    private String filePath;
    private String fileName;
    private String fileSize;
    private String fileType;
}
