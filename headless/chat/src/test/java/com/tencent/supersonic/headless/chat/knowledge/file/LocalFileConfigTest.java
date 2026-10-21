package com.tencent.supersonic.headless.chat.knowledge.file;

import com.tencent.supersonic.headless.chat.knowledge.helper.HanlpHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalFileConfigTest {

    @Test
    void backupDirectoryDefaultIsRelativeToHanlpPropertiesDirectory() throws NoSuchFieldException {
        Field field = LocalFileConfig.class.getDeclaredField("dictDirectoryBackup");
        Value value = field.getAnnotation(Value.class);

        assertEquals("${s2.dict.directory.backup:/data/dictionary/backup}", value.value());
    }

    @Test
    void missingHanlpPropertiesKeepsDefaultDirectoriesRelative() throws Exception {
        LocalFileConfig config = defaults();
        withHanlpProperties(null, () -> {
            assertEquals("./data/dictionary/backup", config.getDictDirectoryBackup());
            assertEquals("./data/dictionary/custom", config.getDictDirectoryLatest());
        });
    }

    @Test
    void existingHanlpPropertiesPrefixesBothDefaultDirectories(@TempDir Path directory)
            throws Exception {
        Path properties = Files.createFile(directory.resolve("hanlp.properties"));
        LocalFileConfig config = defaults();
        withHanlpProperties(properties.toUri().toURL(), () -> {
            assertEquals(directory + "/data/dictionary/backup", config.getDictDirectoryBackup());
            assertEquals(directory + "/data/dictionary/custom", config.getDictDirectoryLatest());
        });
    }

    private LocalFileConfig defaults() throws Exception {
        LocalFileConfig config = new LocalFileConfig();
        for (String name : new String[] {"dictDirectoryLatest", "dictDirectoryBackup"}) {
            Field field = LocalFileConfig.class.getDeclaredField(name);
            String expression = field.getAnnotation(Value.class).value();
            field.setAccessible(true);
            field.set(config,
                    expression.substring(expression.indexOf(':') + 1, expression.length() - 1));
        }
        return config;
    }

    private void withHanlpProperties(URL resource, Runnable assertion)
            throws ClassNotFoundException {
        // Initialize HanLP against the normal classpath before replacing resource lookup.
        Class.forName(HanlpHelper.class.getName(), true, HanlpHelper.class.getClassLoader());
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(new ClassLoader(original) {
            @Override
            public URL getResource(String name) {
                return "hanlp.properties".equals(name) ? resource : super.getResource(name);
            }
        });
        try {
            assertion.run();
        } finally {
            thread.setContextClassLoader(original);
        }
    }
}
