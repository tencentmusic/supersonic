package dev.langchain4j.inmemory.spring;

import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.EmbeddingStoreConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.BaseEmbeddingStoreFactory;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class InMemoryEmbeddingStoreFactory extends BaseEmbeddingStoreFactory {

    public static final String PERSISTENT_FILE_PRE = "InMemory.";
    private EmbeddingStoreProperties embeddingStore;

    public InMemoryEmbeddingStoreFactory(EmbeddingStoreConfig storeConfig) {
        this(createPropertiesFromConfig(storeConfig));
    }

    public InMemoryEmbeddingStoreFactory(EmbeddingStoreProperties embeddingStore) {
        this.embeddingStore = embeddingStore;
    }

    private static EmbeddingStoreProperties createPropertiesFromConfig(
            EmbeddingStoreConfig storeConfig) {
        EmbeddingStoreProperties embeddingStore = new EmbeddingStoreProperties();
        BeanUtils.copyProperties(storeConfig, embeddingStore);
        return embeddingStore;
    }

    @Override
    public synchronized EmbeddingStore createEmbeddingStore(String collectionName) {
        InMemoryEmbeddingStore<TextSegment> embeddingStore = reloadFromPersistFile(collectionName);
        if (Objects.isNull(embeddingStore)) {
            embeddingStore = new InMemoryEmbeddingStore();
        }
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
            if (Files.exists(filePath)
                    && !collectionName.equals(embeddingConfig.getMetaCollectionName())
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
        if (MapUtils.isEmpty(super.collectionNameToStore)) {
            return;
        }
        for (Map.Entry<String, EmbeddingStore<TextSegment>> entry : collectionNameToStore
                .entrySet()) {
            Path filePath = getPersistPath(entry.getKey());
            if (Objects.isNull(filePath)) {
                continue;
            }
            try {
                Path directoryPath = filePath.getParent();
                if (!Files.exists(directoryPath)) {
                    Files.createDirectories(directoryPath);
                }
                if (entry.getValue() instanceof InMemoryEmbeddingStore) {
                    InMemoryEmbeddingStore<TextSegment> inMemoryEmbeddingStore =
                            (InMemoryEmbeddingStore) entry.getValue();
                    inMemoryEmbeddingStore.serializeToFile(filePath);
                }
            } catch (Exception e) {
                log.error("persistFile error, persistFile:" + filePath, e);
            }
        }
    }

    private Path getPersistPath(String collectionName) {
        String persistFile = PERSISTENT_FILE_PRE + collectionName;
        String persistPath = embeddingStore.getPersistPath();
        if (StringUtils.isEmpty(persistPath)) {
            return null;
        }
        return Paths.get(persistPath, persistFile);
    }
}
