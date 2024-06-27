package com.tencent.supersonic.headless.chat.knowledge.file;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.headless.api.pojo.request.DictValueReq;
import com.tencent.supersonic.headless.api.pojo.response.DictValueResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class FileHandlerImpl implements FileHandler {
    public static final String FILE_SPILT = File.separator;

    private final LocalFileConfig localFileConfig;

    public FileHandlerImpl(LocalFileConfig localFileConfig) {
        this.localFileConfig = localFileConfig;
    }

    @Override
    public void backupFile(String fileName) {
        String dictDirectoryBackup = localFileConfig.getDictDirectoryBackup();
        if (!existPath(dictDirectoryBackup)) {
            createDir(dictDirectoryBackup);
        }

        String source = localFileConfig.getDictDirectoryLatest() + FILE_SPILT + fileName;
        String target = dictDirectoryBackup + FILE_SPILT + fileName;
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
    public PageInfo<DictValueResp> queryDictValue(String fileName, DictValueReq dictValueReq) {
        PageInfo<DictValueResp> dictValueRespPageInfo = new PageInfo<>();
        String filePath = localFileConfig.getDictDirectoryLatest() + FILE_SPILT + fileName;
        Long fileLineNum = getFileLineNum(filePath);
        Integer startLine = (dictValueReq.getCurrent() - 1) * dictValueReq.getPageSize() + 1;
        Integer endLine = Integer.valueOf(
                Math.min(dictValueReq.getCurrent() * dictValueReq.getPageSize(), fileLineNum) + "");
        List<DictValueResp> dictValueRespList = getFileData(filePath, startLine, endLine);

        dictValueRespPageInfo.setPageSize(dictValueReq.getPageSize());
        dictValueRespPageInfo.setPageNum(dictValueReq.getCurrent());
        dictValueRespPageInfo.setTotal(fileLineNum);
        dictValueRespPageInfo.setList(dictValueRespList);
        dictValueRespPageInfo.setHasNextPage(endLine >= fileLineNum ? false : true);
        dictValueRespPageInfo.setHasPreviousPage(startLine <= 0 ? false : true);
        return dictValueRespPageInfo;
    }

    @Override
    public String queryDictFilePath(String fileName) {
        String path = localFileConfig.getDictDirectoryLatest() + FILE_SPILT + fileName;
        if (existPath(path)) {
            return path;
        }
        log.info("dict file:{} is not exist", path);
        return null;
    }

    private List<DictValueResp> getFileData(String filePath, Integer startLine, Integer endLine) {
        List<DictValueResp> fileData = new ArrayList<>();

        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            fileData = lines
                    .skip(startLine - 1)
                    .limit(endLine - startLine + 1)
                    .map(lineStr -> convert2Resp(lineStr))
                    .filter(line -> Objects.nonNull(line))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.warn("[getFileData] e:{}", e);
        }
        return fileData;

    }

    private DictValueResp convert2Resp(String lineStr) {
        DictValueResp dictValueResp = new DictValueResp();
        if (StringUtils.isNotEmpty(lineStr)) {
            String[] itemArray = lineStr.split("\\s+");
            if (Objects.nonNull(itemArray) && itemArray.length >= 3) {
                dictValueResp.setValue(itemArray[0].replace("#", " "));
                dictValueResp.setNature(itemArray[1]);
                dictValueResp.setFrequency(Long.parseLong(itemArray[2]));
            }
        }
        return dictValueResp;
    }

    private Long getFileLineNum(String filePath) {
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            Long lineCount = lines
                    .count();
            return lineCount;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0L;
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
        if (CollectionUtils.isEmpty(lines)) {
            log.info("lines is empty");
            return;
        }
        String dictDirectoryLatest = localFileConfig.getDictDirectoryLatest();
        if (!existPath(dictDirectoryLatest)) {
            createDir(dictDirectoryLatest);
        }
        String filePath = dictDirectoryLatest + FILE_SPILT + fileName;
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
        deleteFile(localFileConfig.getDictDirectoryLatest() + FILE_SPILT + fileName);
        return true;
    }

    private BufferedWriter getWriter(String filePath, Boolean append) throws IOException {
        if (append) {
            return Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        }
        return Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8);
    }
}