package com.tencent.supersonic.semantic.model.domain.listener;

import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.common.pojo.DataEvent;
import com.tencent.supersonic.common.pojo.enums.DictWordType;
import com.tencent.supersonic.common.pojo.enums.EventType;
import com.tencent.supersonic.common.util.embedding.EmbeddingQuery;
import com.tencent.supersonic.common.util.embedding.EmbeddingUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@Slf4j
public class MetaEmbeddingListener implements ApplicationListener<DataEvent> {

    public static final String COLLECTION_NAME = "meta_collection";

    @Autowired
    private EmbeddingUtils embeddingUtils;

    @Override
    public void onApplicationEvent(DataEvent event) {
        if (CollectionUtils.isEmpty(event.getDataItems())) {
            return;
        }
        List<EmbeddingQuery> embeddingQueries = event.getDataItems()
                .stream()
                .map(dataItem -> {
                    EmbeddingQuery embeddingQuery = new EmbeddingQuery();
                    embeddingQuery.setQueryId(
                            dataItem.getId().toString() + DictWordType.NATURE_SPILT + dataItem.getType().getName());
                    embeddingQuery.setQuery(dataItem.getName());
                    Map meta = JSONObject.parseObject(JSONObject.toJSONString(dataItem), Map.class);
                    embeddingQuery.setMetadata(meta);
                    embeddingQuery.setQueryEmbedding(null);
                    return embeddingQuery;
                }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(embeddingQueries)) {
            return;
        }
        embeddingUtils.addCollection(COLLECTION_NAME);
        if (event.getEventType().equals(EventType.ADD)) {
            embeddingUtils.addQuery(COLLECTION_NAME, embeddingQueries);
        } else if (event.getEventType().equals(EventType.DELETE)) {
            embeddingUtils.deleteQuery(COLLECTION_NAME, embeddingQueries);
        } else if (event.getEventType().equals(EventType.UPDATE)) {
            embeddingUtils.deleteQuery(COLLECTION_NAME, embeddingQueries);
            embeddingUtils.addQuery(COLLECTION_NAME, embeddingQueries);
        }
    }

}
