package com.tencent.supersonic.chat.api.pojo.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUpLoadReq {

    // 资源文件
    private MultipartFile file;

    // 资源文件类型：VIDEO/IMAGE/AUDIO等
    private String fileType;

    // 默认值deepseek_whole
    private String serviceName = "deepseek_whole";

    // 默认值file_parse
    private String serviceType = "file_parse";
}
