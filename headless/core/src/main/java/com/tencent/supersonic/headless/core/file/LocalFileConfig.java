package com.tencent.supersonic.headless.core.file;

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

    @Value("${dict.directory.backup:./data/dictionary/backup}")
    private String dictDirectoryBackup;

    public String getDictDirectoryLatest() {
        return getResourceDir() + dictDirectoryLatest;
    }

    public String getDictDirectoryBackup() {
        return getResourceDir() + dictDirectoryBackup;
    }

    private String getResourceDir() {
        //return hanlpPropertiesPath = HanlpHelper.getHanlpPropertiesPath();
        return ClassLoader.getSystemClassLoader().getResource("").getPath();
    }

}