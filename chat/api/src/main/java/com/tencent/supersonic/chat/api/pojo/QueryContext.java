package com.tencent.supersonic.chat.api.pojo;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.request.QueryRequest;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QueryContext {

    private QueryRequest request;
    private List<SemanticQuery> candidateQueries = new ArrayList<>();
    private SchemaMapInfo mapInfo = new SchemaMapInfo();

    public QueryContext(QueryRequest request) {
        this.request = request;
    }
}
