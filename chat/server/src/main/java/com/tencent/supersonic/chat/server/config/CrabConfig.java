package com.tencent.supersonic.chat.server.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
@Component
public class CrabConfig {
    @Value("${crab.api.appId}")
    private String appId;
    @Value("${crab.api.secretKey}")
    private String secretKey;
    @Value("${crab.api.host}")
    private String host;
    @Value("${crab.api.file.urls.upload:/crab/api/resource/aimanager/task/upload}")
    private String uploadUrl;
    @Value("${crab.api.file.urls.parse:/crab/api/aigc/task/bigmodel/deep/file/aysn/parse}")
    private String parseUrl;
    @Value("${crab.api.file.urls.status:/crab/api/aigc/task/bigmodel/deep/file/aysn/status}")
    private String statusUrl;
    @Value("${crab.api.deepseek.url:/crab/api/aigc/task/bigmodel/deepseek}")
    private String deepseekUrl;
    @Value("${crab.api.file.serviceName:deepseek_whole}")
    private String fileServiceName;
    @Value("${crab.api.file.serviceType:file_parse}")
    private String fileServiceType;
    @Value("${crab.api.deepseek.serviceName:deepseek_14B}")
    private String dsServiceName;
    @Value("${crab.api.deepseek.serviceType:text_to_text}")
    private String dsServiceType;
    public static final String VIDEO = "VIDEO";
    public static final String IMAGE = "IMAGE";
    public static final String AUDIO = "AUDIO";
    private static final Map<String, String> EXTENSION_TYPE_MAP = initTypeMap();

    private static Map<String, String> initTypeMap() {
        Map<String, String> map = new HashMap<>();
        // 视频类型
        addExtensions(map, VIDEO, "mp4", "avi", "mov", "wmv", "flv", "mkv", "mpeg");
        // 图片类型
        addExtensions(map, IMAGE, "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg");
        // 音频类型
        addExtensions(map, AUDIO, "mp3", "wav", "aac", "flac", "ogg", "m4a");
        return Collections.unmodifiableMap(map);
    }

    private static void addExtensions(Map<String, String> map, String type, String... extensions) {
        for (String ext : extensions) {
            map.put(ext.toLowerCase(), type);
        }
    }

    public static String getFileType(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return EXTENSION_TYPE_MAP.getOrDefault(ext, ext);
    }
    public String getDsModel(String serviceName) {
        // 根据文档中的对应关系设置
        return switch (serviceName) {
            case "deepseek_14B" -> "default";
            case "deepseek_70B" -> "DeepSeek70";
            case "deepseek_whole" -> "DeepSeekR1";
            case "deepseek_whole_quantify" -> "DeepSeekR1";
            case "deepseek_32B" -> "DeepSeek32";
            case "deepseek_V3" -> "DeepSeekV3";
            default -> "default";
        };
    }
}
