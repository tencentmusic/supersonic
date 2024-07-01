package dev.langchain4j.inmemory.spring;

import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreFactory;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class InMemoryEmbeddingStoreFactory implements EmbeddingStoreFactory {

    public static final String PERSISTENT_FILE_PRE = "InMemory.";
    private static Map<String, InMemoryEmbeddingStore<TextSegment>> collectionNameToStore =
            new ConcurrentHashMap<>();
    private Properties properties;


    public InMemoryEmbeddingStoreFactory(Properties properties) {
        this.properties = properties;
    }

    @Override
    public synchronized EmbeddingStore create(String collectionName) {
        InMemoryEmbeddingStore<TextSegment> embeddingStore = collectionNameToStore.get(collectionName);
        if (Objects.nonNull(embeddingStore)) {
            return embeddingStore;
        }
        embeddingStore = reloadFromPersistFile(collectionName);
        if (Objects.isNull(embeddingStore)) {
            embeddingStore = new InMemoryEmbeddingStore();
        }
        collectionNameToStore.putIfAbsent(collectionName, embeddingStore);
        return embeddingStore;

    }

    private InMemoryEmbeddingStore<TextSegment> reloadFromPersistFile(String collectionName) {
        Path filePath = getPersistPath(collectionName);
        if (Objects.isNull(filePath)) {
            return null;
        }
        InMemoryEmbeddingStore<TextSegment> embeddingStore = null;
        try {
            EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
            if (Files.exists(filePath) && !collectionName.equals(embeddingConfig.getMetaCollectionName())
                    && !collectionName.equals(embeddingConfig.getText2sqlCollectionName())) {
                embeddingStore = InMemoryEmbeddingStore.fromFile(filePath);
                embeddingStore.entries = new CopyOnWriteArraySet<>(embeddingStore.entries);
                log.info("embeddingStore reload from file:{}", filePath);
            }
        } catch (Exception e) {
            log.error("load persistFile error, persistFile:" + filePath, e);
        }
        return embeddingStore;
    }

    public synchronized void persistFile() {
        if (MapUtils.isEmpty(collectionNameToStore)) {
            return;
        }
        for (Map.Entry<String, InMemoryEmbeddingStore<TextSegment>> entry : collectionNameToStore.entrySet()) {
            Path filePath = getPersistPath(entry.getKey());
            if (Objects.isNull(filePath)) {
                continue;
            }
            try {
                Path directoryPath = filePath.getParent();
                if (!Files.exists(directoryPath)) {
                    Files.createDirectories(directoryPath);
                }
                entry.getValue().serializeToFile(filePath);
            } catch (Exception e) {
                log.error("persistFile error, persistFile:" + filePath, e);
            }
        }
    }

    private Path getPersistPath(String collectionName) {
        String persistFile = PERSISTENT_FILE_PRE + collectionName;
        String persistPath = properties.getEmbeddingStore().getPersistPath();
        if (StringUtils.isEmpty(persistPath)) {
            return null;
        }
        return Paths.get(persistPath, persistFile);
    }

}