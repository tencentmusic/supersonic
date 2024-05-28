package com.tencent.supersonic.headless.api.pojo.response;

import com.tencent.supersonic.common.pojo.QueryAuthorization;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.headless.api.pojo.AggregateInfo;
import com.tencent.supersonic.headless.api.pojo.EntityInfo;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QueryResult {
    private Long queryId;
    private String queryMode;
    private String querySql;
    private QueryState queryState = QueryState.EMPTY;
    private List<QueryColumn> queryColumns;
    private QueryAuthorization queryAuthorization;
    private SemanticParseInfo chatContext;
    private Object response;
    private List<Map<String, Object>> queryResults;
    private String textResult;
    private Long queryTimeCost;
    private EntityInfo entityInfo;
    private List<SchemaElement> recommendedDimensions;
    private AggregateInfo aggregateInfo;
}
