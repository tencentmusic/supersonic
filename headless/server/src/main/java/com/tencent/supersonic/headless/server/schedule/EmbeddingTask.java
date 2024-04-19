package com.tencent.supersonic.headless.server.schedule;

import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.DataItem;
import com.tencent.supersonic.common.util.ComponentFactory;
import com.tencent.supersonic.common.util.embedding.EmbeddingQuery;
import com.tencent.supersonic.common.util.embedding.InMemoryS2EmbeddingStore;
import com.tencent.supersonic.common.util.embedding.S2EmbeddingStore;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.MetricService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.List;

@Component
@Slf4j
public class EmbeddingTask {

    private S2EmbeddingStore s2EmbeddingStore = ComponentFactory.getS2EmbeddingStore();
    @Autowired
    private EmbeddingConfig embeddingConfig;
    @Autowired
    private MetricService metricService;

    @Autowired
    private DimensionService dimensionService;

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


    /***
     * reload meta embedding
     */
    @Scheduled(cron = "${reload.meta.embedding.corn:0 0 */2 * * ?}")
    public void reloadMetaEmbedding() {
        log.info("reload.meta.embedding start");
        try {
            List<DataItem> metricDataItems = metricService.getDataEvent().getDataItems();

            s2EmbeddingStore.addQuery(embeddingConfig.getMetaCollectionName(),
                    EmbeddingQuery.convertToEmbedding(metricDataItems));

            List<DataItem> dimensionDataItems = dimensionService.getDataEvent().getDataItems();
            s2EmbeddingStore.addQuery(embeddingConfig.getMetaCollectionName(),
                    EmbeddingQuery.convertToEmbedding(dimensionDataItems));
        } catch (Exception e) {
            log.error("reload.meta.embedding error", e);
        }

        log.info("reload.meta.embedding end");
    }
}