package com.tencent.supersonic.common.util;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * * tools functions to file
 */
public class FileUtils {

    public static Boolean exit(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static long getLastModified(String path) {
        if (!exit(path)) {
            return -1;
        }
        File file = new File(path);
        Optional<Long> lastModified = Arrays.stream(file.listFiles()).filter(f -> f.isFile())
                .map(f -> f.lastModified()).sorted(Collections.reverseOrder()).findFirst();

        if (lastModified.isPresent()) {
            return lastModified.get();
        }
        return -1;
    }

    public static File[] getDirFiles(File file) {
        if (file.isDirectory()) {
            return file.listFiles();
        }
        return null;
    }

    public static void scanDirectory(File file, int maxLevel, Map<Integer, List<File>> directories) {
        if (maxLevel < 0) {
            return;
        }
        if (!file.exists() || !file.isDirectory()) {
            return;
        }
        if (!directories.containsKey(maxLevel)) {
            directories.put(maxLevel, new ArrayList<>());
        }
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                directories.get(maxLevel).add(f);
                scanDirectory(f, maxLevel - 1, directories);
            }
        }
    }

    public static Map<String, Map<String, List<String>>> getTop3Directory(String path) {
        Map<String, Map<String, List<String>>> result = new HashMap<>();
        File file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            return result;
        }
        Map<Integer, List<File>> directories = new HashMap<>();
        scanDirectory(file, 2, directories);
        for (int i = 2; i >= 0; i--) {
            for (File f : directories.getOrDefault(i, new ArrayList<>())) {
                if (i == 2) {
                    result.put(f.getName(), new HashMap<>());
                    continue;
                }
                if (i == 1 && result.containsKey(f.getParentFile().getName())) {
                    result.get(f.getParentFile().getName()).put(f.getName(), new ArrayList<>());
                    continue;
                }
                String parent = f.getParentFile().getParentFile().getName();
                if (result.containsKey(parent) && result.get(parent).containsKey(f.getParentFile().getName())) {
                    result.get(parent).get(f.getParentFile().getName()).add(f.getName());
                }
            }
        }
        return result;
    }

}
