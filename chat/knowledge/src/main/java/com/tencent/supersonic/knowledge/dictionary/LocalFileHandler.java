package com.tencent.supersonic.knowledge.dictionary;


import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class LocalFileHandler implements FileHandler {

    private final LocalFileConfig localFileConfig;

    public LocalFileHandler(LocalFileConfig localFileConfig) {
        this.localFileConfig = localFileConfig;
    }

    @Override
    public void backupFile(String fileName) {
        String dictDirectoryBackup = localFileConfig.getDictDirectoryBackup();
        if (!existPath(dictDirectoryBackup)) {
            createDir(dictDirectoryBackup);
        }

        String source = localFileConfig.getDictDirectoryLatest() + "/" + fileName;
        String target = dictDirectoryBackup + "/" + fileName;
        Path sourcePath = Paths.get(source);
        Path targetPath = Paths.get(target);
        try {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("backupFile successfully! path:{}", targetPath.toAbsolutePath());
        } catch (IOException e) {
            log.info("Failed to copy file: " + e.getMessage());
        }

    }

    @Override
    public void createDir(String directoryPath) {
        Path path = Paths.get(directoryPath);
        try {
            Files.createDirectories(path);
            log.info("Directory created successfully!");
        } catch (IOException e) {
            log.info("Failed to create directory: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String filePath) {
        Path path = Paths.get(filePath);
        try {
            Files.delete(path);
            log.info("File:{} deleted successfully!", getAbsolutePath(filePath));
        } catch (IOException e) {
            log.warn("Failed to delete file:{}, e:", getAbsolutePath(filePath), e);
        }
    }

    @Override
    public Boolean existPath(String pathStr) {
        Path path = Paths.get(pathStr);
        if (Files.exists(path)) {
            log.info("path:{} exists!", getAbsolutePath(pathStr));
            return true;
        } else {
            log.info("path:{} not exists!", getAbsolutePath(pathStr));
        }
        return false;
    }

    @Override
    public void writeFile(List<String> lines, String fileName, Boolean append) {
        String dictDirectoryLatest = localFileConfig.getDictDirectoryLatest();
        if (!existPath(dictDirectoryLatest)) {
            createDir(dictDirectoryLatest);
        }
        String filePath = dictDirectoryLatest + "/" + fileName;
        if (existPath(filePath)) {
            backupFile(fileName);
        }
        try (BufferedWriter writer = getWriter(filePath, append)) {
            if (!CollectionUtils.isEmpty(lines)) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            log.info("File:{} written successfully!", getAbsolutePath(filePath));
        } catch (IOException e) {
            log.info("Failed to write file:{}, e:", getAbsolutePath(filePath), e);
        }
    }

    public String getAbsolutePath(String path) {
        return Paths.get(path).toAbsolutePath().toString();
    }


    @Override
    public String getDictRootPath() {
        return Paths.get(localFileConfig.getDictDirectoryLatest()).toAbsolutePath().toString();
    }

    @Override
    public Boolean deleteDictFile(String fileName) {
        backupFile(fileName);
        deleteFile(localFileConfig.getDictDirectoryLatest() + "/" + fileName);
        return true;
    }

    private BufferedWriter getWriter(String filePath, Boolean append) throws IOException {
        if (append) {
            return Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        }
        return Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8);
    }
}
