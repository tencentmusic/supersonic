package com.tencent.supersonic.chat.server.rest;

import com.tencent.supersonic.chat.api.pojo.request.FileReq;
import com.tencent.supersonic.chat.server.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chat/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/uploadAndParse")
    public Object uploadFile(@RequestParam("file") MultipartFile file) {
        return fileService.uploadAndParse(file);
    }

    @PostMapping("/status")
    public Object getStatus(@RequestBody FileReq fileReq) {
        return fileService.getStatus(fileReq);
    }
}
