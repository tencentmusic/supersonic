package com.tencent.supersonic.common.util.embedding;


import lombok.Data;

import java.util.List;

@Data
public class RetrieveQueryResult {

    private String query;

    private List<Retrieval> retrieval;

}
