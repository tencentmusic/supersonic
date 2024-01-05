package com.tencent.supersonic.chat.core.parser.plugin.embedding;


import lombok.Data;

import java.util.List;

@Data
public class RecallRetrievalResp {

    private String query;

    private List<RecallRetrieval> retrieval;


}
