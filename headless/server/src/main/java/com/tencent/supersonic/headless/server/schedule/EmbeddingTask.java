package com.tencent.supersonic.headless.server.schedule;

import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.DataItem;
import com.tencent.supersonic.common.service.EmbeddingService;
import com.tencent.supersonic.headless.server.web.service.DimensionService;
import com.tencent.supersonic.headless.server.web.service.MetricService;
import dev.langchain4j.store.embedding.TextSegmentConvert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.List;

@Component
@Slf4j
public class EmbeddingTask {

    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private EmbeddingConfig embeddingConfig;
    @Autowired
    private MetricService metricService;

    @Autowired
    private DimensionService dimensionService;

    @PreDestroy
    public void onShutdown() {
        //        embeddingStorePersistentToFile();
    }

    //    private void embeddingStorePersistentToFile() {
    //        if (embeddingService instanceof InMemoryEmbeddingService) {
    //            log.info("start persistentToFile");
    //            ((InMemoryEmbeddingService) embeddingService).persistentToFile();
    //            log.info("end persistentToFile");
    //        }
    //    }

    @Scheduled(cron = "${inMemoryEmbeddingStore.persistent.cron:0 0 * * * ?}")
    public void executeTask() {
        //        embeddingStorePersistentToFile();
    }

    /***
     * reload meta embedding
     */
    @Scheduled(cron = "${reload.meta.embedding.corn:0 0 */2 * * ?}")
    public void reloadMetaEmbedding() {
        log.info("reload.meta.embedding start");
        try {
            List<DataItem> metricDataItems = metricService.getDataEvent().getDataItems();

            embeddingService.addQuery(embeddingConfig.getMetaCollectionName(),
                    TextSegmentConvert.convertToEmbedding(metricDataItems));

            List<DataItem> dimensionDataItems = dimensionService.getDataEvent().getDataItems();
            embeddingService.addQuery(embeddingConfig.getMetaCollectionName(),
                    TextSegmentConvert.convertToEmbedding(dimensionDataItems));
        } catch (Exception e) {
            log.error("reload.meta.embedding error", e);
        }

        log.info("reload.meta.embedding end");
    }
}