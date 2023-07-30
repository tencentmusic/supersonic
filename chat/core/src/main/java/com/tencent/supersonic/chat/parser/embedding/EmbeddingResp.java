package com.tencent.supersonic.chat.parser.embedding;


import lombok.Data;

import java.util.List;

@Data
public class EmbeddingResp {

    private String query;

    private List<RecallRetrieval> retrieval;


}
