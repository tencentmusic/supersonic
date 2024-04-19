package com.tencent.supersonic.common.util.embedding;


import com.alibaba.fastjson.JSONObject;
import com.tencent.supersonic.common.pojo.DataItem;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class EmbeddingQuery {

    private String queryId;

    private String query;

    private Map<String, Object> metadata;

    private List<Double> queryEmbedding;

    public static List<EmbeddingQuery> convertToEmbedding(List<DataItem> dataItems) {
        return dataItems.stream().map(dataItem -> {
            EmbeddingQuery embeddingQuery = new EmbeddingQuery();
            embeddingQuery.setQueryId(
                    dataItem.getId() + dataItem.getType().name().toLowerCase());
            embeddingQuery.setQuery(dataItem.getName());
            Map meta = JSONObject.parseObject(JSONObject.toJSONString(dataItem), Map.class);
            embeddingQuery.setMetadata(meta);
            embeddingQuery.setQueryEmbedding(null);
            return embeddingQuery;
        }).collect(Collectors.toList());
    }

}
