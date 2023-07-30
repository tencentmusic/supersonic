package com.tencent.supersonic.knowledge.dictionary;

import com.tencent.supersonic.knowledge.utils.HanlpHelper;
import java.io.FileNotFoundException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
@Slf4j
public class LocalFileConfig {


    @Value("${dict.directory.latest:/data/dictionary/custom}")
    private String dictDirectoryLatest;

    @Value("${dict.directory.backup:./dict/backup}")
    private String dictDirectoryBackup;

    public String getDictDirectoryLatest() {
        return getResourceDir() + dictDirectoryLatest;
    }

    public String getDictDirectoryBackup() {
        return dictDirectoryBackup;
    }

    private String getResourceDir() {
        String hanlpPropertiesPath = "";
        try {
            hanlpPropertiesPath = HanlpHelper.getHanlpPropertiesPath();
        } catch (FileNotFoundException e) {
            log.warn("getResourceDir, e:", e);
        }
        return hanlpPropertiesPath;
    }
}