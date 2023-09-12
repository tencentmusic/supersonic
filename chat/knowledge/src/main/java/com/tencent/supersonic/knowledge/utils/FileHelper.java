package com.tencent.supersonic.knowledge.utils;

import static com.hankcs.hanlp.HanLP.Config.CustomDictionaryPath;

import com.hankcs.hanlp.dictionary.DynamicCustomDictionary;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileHelper {

    public static final String FILE_SPILT = "/";

    public static void deleteCacheFile(String[] path) throws IOException {

        String customPath = getCustomPath(path);
        File customFolder = new File(customPath);

        File[] customSubFiles = getFileList(customFolder, ".bin");

        for (File file : customSubFiles) {
            try {
                file.delete();
                log.info("customPath:{},delete cache file:{}", customPath, file);
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
        return path[0].substring(0, path[0].lastIndexOf(FILE_SPILT)) + FILE_SPILT;
    }

    /**
     * reset path
     *
     * @param customDictionary
     */
    public static void resetCustomPath(DynamicCustomDictionary customDictionary) {
        String[] path = CustomDictionaryPath;

        String customPath = getCustomPath(path);
        File customFolder = new File(customPath);

        File[] customSubFiles = getFileList(customFolder, ".txt");

        List<String> fileList = new ArrayList<>();

        for (File file : customSubFiles) {
            if (file.isFile()) {
                fileList.add(file.getAbsolutePath());
            }
        }

        log.info("CustomDictionaryPath:{}", fileList);
        CustomDictionaryPath = fileList.toArray(new String[0]);
        customDictionary.path = (CustomDictionaryPath == null || CustomDictionaryPath.length == 0) ? path
                : CustomDictionaryPath;
        if (CustomDictionaryPath == null || CustomDictionaryPath.length == 0) {
            CustomDictionaryPath = path;
        }
    }
}
