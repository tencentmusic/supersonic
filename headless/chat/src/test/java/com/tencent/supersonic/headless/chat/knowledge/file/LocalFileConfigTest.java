package com.tencent.supersonic.headless.chat.knowledge.file;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalFileConfigTest {

    @Test
    void backupDirectoryDefaultIsRelativeToHanlpPropertiesDirectory() throws NoSuchFieldException {
        Field field = LocalFileConfig.class.getDeclaredField("dictDirectoryBackup");
        Value value = field.getAnnotation(Value.class);

        assertEquals("${s2.dict.directory.backup:/data/dictionary/backup}", value.value());
    }
}
