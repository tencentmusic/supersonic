package com.tencent.supersonic.common.util.embedding;


import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class EmbeddingQuery {

    private String queryId;

    private String query;

    private Map<String, String> metadata;

    private List<Double> queryEmbedding;

}
