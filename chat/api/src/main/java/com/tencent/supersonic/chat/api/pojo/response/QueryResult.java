package com.tencent.supersonic.chat.api.pojo.response;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.common.pojo.QueryAuthorization;
import com.tencent.supersonic.common.pojo.QueryColumn;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class QueryResult {

    public EntityInfo entityInfo;
    public AggregateInfo aggregateInfo;
    private Long queryId;
    private String queryMode;
    private String querySql;
    private QueryState queryState = QueryState.EMPTY;
    private List<QueryColumn> queryColumns;
    private QueryAuthorization queryAuthorization;
    private SemanticParseInfo chatContext;
    private Object response;
    private List<Map<String, Object>> queryResults;
    private Long queryTimeCost;
    private List<SchemaElement> recommendedDimensions;
}
