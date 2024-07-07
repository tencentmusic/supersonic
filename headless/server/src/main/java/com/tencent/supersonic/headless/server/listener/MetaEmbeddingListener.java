package com.tencent.supersonic.headless.server.listener;

import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.pojo.DataEvent;
import com.tencent.supersonic.common.pojo.DataItem;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.common.service.EmbeddingService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.TextSegmentConvert;
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

    @Autowired
    private EmbeddingService embeddingService;

    @Value("${s2.embedding.operation.sleep.time:3000}")
    private Integer embeddingOperationSleepTime;

    @Async
    @Override
    public void onApplicationEvent(DataEvent event) {
        List<DataItem> dataItems = event.getDataItems();
        if (CollectionUtils.isEmpty(dataItems)) {
            return;
        }
        List<TextSegment> textSegments = TextSegmentConvert.convertToEmbedding(dataItems);
        if (CollectionUtils.isEmpty(textSegments)) {
            return;
        }
        sleep();
        if (event.getEventType().equals(EventType.ADD)) {
            embeddingService.addQuery(embeddingConfig.getMetaCollectionName(), textSegments);
        } else if (event.getEventType().equals(EventType.DELETE)) {
            embeddingService.deleteQuery(embeddingConfig.getMetaCollectionName(), textSegments);
        } else if (event.getEventType().equals(EventType.UPDATE)) {
            embeddingService.deleteQuery(embeddingConfig.getMetaCollectionName(), textSegments);
            embeddingService.addQuery(embeddingConfig.getMetaCollectionName(), textSegments);
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
