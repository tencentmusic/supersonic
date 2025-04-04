package com.tencent.supersonic.chat.server.service;

import com.tencent.supersonic.chat.api.pojo.request.FileReq;
import com.tencent.supersonic.chat.api.pojo.response.FileBaseResponse;
import com.tencent.supersonic.chat.api.pojo.response.FileParseResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileBaseResponse<FileParseResponse> uploadAndParse(MultipartFile file);

    Object getStatus(FileReq fileReq);
}
