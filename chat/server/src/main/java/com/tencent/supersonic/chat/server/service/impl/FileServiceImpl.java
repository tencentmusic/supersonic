package com.tencent.supersonic.chat.server.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.supersonic.chat.api.pojo.request.FileReq;
import com.tencent.supersonic.chat.api.pojo.response.FileBaseResponse;
import com.tencent.supersonic.chat.api.pojo.response.FileParseResponse;
import com.tencent.supersonic.chat.api.pojo.response.FileStatusResponse;
import com.tencent.supersonic.chat.api.pojo.response.FileUpLoadResponse;
import com.tencent.supersonic.chat.server.config.CrabConfig;
import com.tencent.supersonic.common.pojo.FileInfo;
import com.tencent.supersonic.chat.server.service.FileService;
import com.tencent.supersonic.common.util.HttpUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.MiguApiUrlUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Autowired
    private CrabConfig crabConfig;


    @Override
    public FileBaseResponse<FileParseResponse> uploadAndParse(MultipartFile file) {
        try {
            String fileType = CrabConfig.getFileType(file.getOriginalFilename());
            log.info("uploadAndParse fileType: {}", fileType);
            // 1. 上传文件
            FileInfo uploadedFile = uploadFile(file, fileType);
            // 2. 构建异步任务参数
            Map<String, Object> taskParams = buildTaskParams(uploadedFile);
            // 3. 文档解析异步下发任务
            return getFileParseResponse(taskParams);
        } catch (Exception e) {
            throw new RuntimeException("操作失败: " + e.getMessage());
        }
    }

    @Override
    public Object getStatus(FileReq fileReq) {
        try {
            Map<String, Object> queryParams = new HashMap<>();
            String signedUrl = MiguApiUrlUtils.doSignature(crabConfig.getStatusUrl(), "GET",
                    queryParams, crabConfig.getAppId(), crabConfig.getSecretKey());
            String fullUrl = crabConfig.getHost() + signedUrl + "&taskId=" + fileReq.getTaskId()
                    + "&serviceName=" + crabConfig.getFileServiceName() + "&serviceType="
                    + crabConfig.getFileServiceType();
            String response = HttpUtils.get(fullUrl);
            return JsonUtil.toObject(response,
                    new TypeReference<FileBaseResponse<FileStatusResponse>>() {});
        } catch (IOException e) {
            throw new RuntimeException("状态查询失败: " + e.getMessage());
        }
    }

    private FileBaseResponse<FileParseResponse> getFileParseResponse(Map<String, Object> taskParams)
            throws IOException {
        Map<String, Object> queryParams = new HashMap<>();
        String signedUrl = MiguApiUrlUtils.doSignature(crabConfig.getParseUrl(), "POST",
                queryParams, crabConfig.getAppId(), crabConfig.getSecretKey());
        String response = HttpUtils.post(crabConfig.getHost() + signedUrl, taskParams);
        return JsonUtil.toObject(response,
                new TypeReference<FileBaseResponse<FileParseResponse>>() {});
    }

    private Map<String, Object> buildTaskParams(FileInfo fileInfo) {
        Map<String, Object> params = new HashMap<>();
        params.put("fileInfoList", List.of(fileInfo));
        return Map.of("serviceName", crabConfig.getFileServiceName(), "serviceType",
                crabConfig.getFileServiceType(), "requestId", UUID.randomUUID().toString(), "params",
                params);
    }

    private FileInfo uploadFile(MultipartFile file, String fileType) {
        try {
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getOriginalFilename(),
                            RequestBody.create(MediaType.parse("application/octet-stream"),
                                    file.getBytes()))
                    .addFormDataPart("fileType", fileType)
                    .addFormDataPart("serviceName", crabConfig.getFileServiceName())
                    .addFormDataPart("serviceType", crabConfig.getFileServiceType()).build();

            Map<String, Object> queryParams = new HashMap<>();
            String signedUrl = MiguApiUrlUtils.doSignature(crabConfig.getUploadUrl(), "POST",
                    queryParams, crabConfig.getAppId(), crabConfig.getSecretKey());
            log.info("uploadFile body: {}", body);
            String response = HttpUtils.postMultipart(crabConfig.getHost() + signedUrl, body);
            FileBaseResponse<FileUpLoadResponse> baseResponse = JsonUtil.toObject(response,
                    new TypeReference<FileBaseResponse<FileUpLoadResponse>>() {});

            if (!"OK".equals(baseResponse.getState())) {
                throw new RuntimeException("上传失败: " + baseResponse.getErrorMessage());
            }
            FileUpLoadResponse fileUpLoadResponse = baseResponse.getBody();
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFilePath(fileUpLoadResponse.getFilePath());
            fileInfo.setFileName(file.getOriginalFilename());
            fileInfo.setFileSize(String.valueOf(file.getSize()));
            fileInfo.setFileType(fileType);
            fileInfo.setFileId(fileUpLoadResponse.getFileId());
            return fileInfo;
        } catch (Exception e) {
            log.error("uploadFile error", e);
            throw new RuntimeException("上传失败: " + e.getMessage());
        }
    }

}
