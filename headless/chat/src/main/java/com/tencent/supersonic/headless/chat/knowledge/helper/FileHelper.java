package com.tencent.supersonic.headless.chat.knowledge.helper;

import com.hankcs.hanlp.HanLP.Config;
import com.hankcs.hanlp.dictionary.DynamicCustomDictionary;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileHelper {

    public static final String FILE_SPILT = File.separator;

    public static void deleteCacheFile(String[] path) throws IOException {

        String customPath = getCustomPath(path);
        File customFolder = new File(customPath);

        File[] customSubFiles = getFileList(customFolder, ".bin");

        for (File file : customSubFiles) {
            try {
                file.delete();
                log.info("customPath:{},delete file:{}", customPath, file);
            } catch (Exception e) {
                log.error("delete " + file, e);
            }
        }
    }

    private static File[] getFileList(File customFolder, String suffix) {
        File[] customSubFiles = customFolder.listFiles(file -> {
            if (file.isDirectory()) {
                return false;
            }
            if (file.getName().toLowerCase().endsWith(suffix)) {
                return true;
            }
            return false;
        });
        return customSubFiles;
    }

    private static String getCustomPath(String[] path) {
        Path firstPath = Paths.get(path[0]).normalize();
        return firstPath.getParent().toString() + File.separator;
    }

    /**
     * reset path
     *
     * @param customDictionary
     */
    public static void resetCustomPath(DynamicCustomDictionary customDictionary) {
        String[] path = Config.CustomDictionaryPath;

        String customPath = getCustomPath(path);
        File customFolder = new File(customPath);

        File[] customSubFiles = getFileList(customFolder, ".txt");

        List<String> fileList = new ArrayList<>();

        for (File file : customSubFiles) {
            if (file.isFile()) {
                fileList.add(file.getAbsolutePath());
            }
        }

        log.debug("CustomDictionaryPath:{}", fileList);
        Config.CustomDictionaryPath = fileList.toArray(new String[0]);
        customDictionary.path =
                (Config.CustomDictionaryPath == null || Config.CustomDictionaryPath.length == 0)
                        ? path
                        : Config.CustomDictionaryPath;
        if (Config.CustomDictionaryPath == null || Config.CustomDictionaryPath.length == 0) {
            Config.CustomDictionaryPath = path;
        }
    }
}
