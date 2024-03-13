package com.tencent.supersonic.headless.core.file;

import com.tencent.supersonic.headless.core.chat.knowledge.helper.HanlpHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;

@Data
@Configuration
@Slf4j
public class LocalFileConfig {


    @Value("${dict.directory.latest:/data/dictionary/custom}")
    private String dictDirectoryLatest;

    @Value("${dict.directory.backup:./data/dictionary/backup}")
    private String dictDirectoryBackup;

    public String getDictDirectoryLatest() {
        return getDictDirectoryPrefixDir() + dictDirectoryLatest;
    }

    public String getDictDirectoryBackup() {
        return getDictDirectoryPrefixDir() + dictDirectoryBackup;
    }

    private String getDictDirectoryPrefixDir() {
        try {
            return HanlpHelper.getHanlpPropertiesPath();
        } catch (FileNotFoundException e) {
            log.warn("getDictDirectoryPrefixDir error: " + e);
            e.printStackTrace();
        }
        return "";
    }

}