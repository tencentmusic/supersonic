package com.tencent.supersonic.common.pojo;

import lombok.Data;

@Data
public class FileInfo {
    private String fileId;
    private String filePath;
    private String fileName;
    private String fileSize;
    private String fileType;
    private String fileContent;
}
