package com.tencent.supersonic.chat.parser.embedding;


import java.util.List;
import lombok.Data;

@Data
public class EmbeddingResp {

    private String query;

    private List<RecallRetrieval> retrieval;


}
