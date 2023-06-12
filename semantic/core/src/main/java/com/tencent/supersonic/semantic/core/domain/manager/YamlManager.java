package com.tencent.supersonic.semantic.core.domain.manager;


import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.semantic.core.domain.config.YamlConfig;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class YamlManager {


    protected final YamlConfig yamlConfig;

//    private final ParserService parserService;


    public YamlManager(YamlConfig yamlConfig
//                      , @Lazy ParserService parserService
    ) {
        this.yamlConfig = yamlConfig;
//        this.parserService = parserService;
    }

    public void generateYamlFile(String yamlStr, String path, String name) throws Exception {
        String localPath = generateLocalYamlPath(path, name);
        File file = createMetaYamlFile(localPath);
        FileUtils.writeStringToFile(file, yamlStr, StandardCharsets.UTF_8);
//        parserService.reloadModels(path);

    }

    public void deleteYamlFile(String path, String fileName) {
        String localPath = generateLocalYamlPath(path, fileName);
        deleteMetaYamlFile(localPath);
    }

    private File createMetaYamlFile(String fullPath) throws IOException {
        File file = new File(fullPath);
        if (file.getParentFile().mkdirs() && file.createNewFile()) {
            log.info("File :{} created: " + fullPath);
        } else {
            log.warn("File:{} create failed.", fullPath);
        }
        return file;
    }

    private void deleteMetaYamlFile(String fullPath) {
        File file = new File(fullPath);
        if (file.delete()) {
            log.info("File :{} deleted: " + fullPath);
        } else {
            log.info("File :{} delete failed: " + fullPath);
        }
    }

    private String generateLocalYamlPath(String path, String name) {
        return yamlConfig.getmetaYamlFileDir() + path + name + Constants.YAML_FILES_SUFFIX;
    }
}
