package com.tencent.supersonic.chat.parser.plugin.embedding;


import lombok.Data;

import java.util.List;

@Data
public class EmbeddingResp {

    private String query;

    private List<RecallRetrieval> retrieval;


}
