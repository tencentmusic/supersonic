package com.tencent.supersonic.chat.api.pojo;

import com.tencent.supersonic.chat.api.component.SemanticQuery;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class QueryContext {

    private QueryReq request;
    private List<SemanticQuery> candidateQueries = new ArrayList<>();
    private SchemaMapInfo mapInfo = new SchemaMapInfo();

    public QueryContext(QueryReq request) {
        this.request = request;
    }
}
