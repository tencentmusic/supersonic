package com.tencent.supersonic.chat.server.plugin.recall.embedding;


import lombok.Data;
import java.util.List;

@Data
public class RecallRetrievalResp {

    private String query;

    private List<RecallRetrieval> retrieval;


}
