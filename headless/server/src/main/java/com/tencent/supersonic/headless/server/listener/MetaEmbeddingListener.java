package com.tencent.supersonic.headless.server.listener;

import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.DataEvent;
import com.tencent.supersonic.common.pojo.DataItem;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.common.util.ComponentFactory;
import com.tencent.supersonic.common.util.embedding.EmbeddingQuery;
import com.tencent.supersonic.common.util.embedding.S2EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
@Slf4j
public class MetaEmbeddingListener implements ApplicationListener<DataEvent> {

    @Autowired
    private EmbeddingConfig embeddingConfig;

    private S2EmbeddingStore s2EmbeddingStore = ComponentFactory.getS2EmbeddingStore();

    @Value("${embedding.operation.sleep.time:3000}")
    private Integer embeddingOperationSleepTime;

    @Async
    @Override
    public void onApplicationEvent(DataEvent event) {
        List<DataItem> dataItems = event.getDataItems();
        if (CollectionUtils.isEmpty(dataItems)) {
            return;
        }
        List<EmbeddingQuery> embeddingQueries = EmbeddingQuery.convertToEmbedding(dataItems);
        if (CollectionUtils.isEmpty(embeddingQueries)) {
            return;
        }
        sleep();
        s2EmbeddingStore.addCollection(embeddingConfig.getMetaCollectionName());
        if (event.getEventType().equals(EventType.ADD)) {
            s2EmbeddingStore.addQuery(embeddingConfig.getMetaCollectionName(), embeddingQueries);
        } else if (event.getEventType().equals(EventType.DELETE)) {
            s2EmbeddingStore.deleteQuery(embeddingConfig.getMetaCollectionName(), embeddingQueries);
        } else if (event.getEventType().equals(EventType.UPDATE)) {
            s2EmbeddingStore.deleteQuery(embeddingConfig.getMetaCollectionName(), embeddingQueries);
            s2EmbeddingStore.addQuery(embeddingConfig.getMetaCollectionName(), embeddingQueries);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(embeddingOperationSleepTime);
        } catch (InterruptedException e) {
            log.error("", e);
        }
    }

}
