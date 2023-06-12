package com.tencent.supersonic.semantic.api.core.response;


import com.tencent.supersonic.semantic.api.core.pojo.QueryAuthorization;
import com.tencent.supersonic.semantic.api.core.pojo.QueryColumn;
import com.tencent.supersonic.semantic.api.core.pojo.QueryResult;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QueryResultWithSchemaResp extends QueryResult<Map<String, Object>> {

    List<QueryColumn> columns;
    String sql;
    QueryAuthorization queryAuthorization;
}
