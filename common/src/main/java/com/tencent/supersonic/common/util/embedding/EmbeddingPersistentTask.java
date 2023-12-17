package com.tencent.supersonic.common.util.embedding;

import com.tencent.supersonic.common.util.ComponentFactory;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmbeddingPersistentTask {

    private S2EmbeddingStore s2EmbeddingStore = ComponentFactory.getS2EmbeddingStore();

    @PreDestroy
    public void onShutdown() {
        embeddingStorePersistentToFile();
    }

    private void embeddingStorePersistentToFile() {
        if (s2EmbeddingStore instanceof InMemoryS2EmbeddingStore) {
            log.info("start persistentToFile");
            ((InMemoryS2EmbeddingStore) s2EmbeddingStore).persistentToFile();
            log.info("end persistentToFile");
        }
    }

    @Scheduled(cron = "${inMemoryEmbeddingStore.persistent.cron:0 0 * * * ?}")
    public void executeTask() {
        embeddingStorePersistentToFile();
    }
}