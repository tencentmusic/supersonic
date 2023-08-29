package com.tencent.supersonic.chat.parser.plugin.embedding;


import java.util.List;
import lombok.Data;

@Data
public class EmbeddingResp {

    private String query;

    private List<RecallRetrieval> retrieval;


}
