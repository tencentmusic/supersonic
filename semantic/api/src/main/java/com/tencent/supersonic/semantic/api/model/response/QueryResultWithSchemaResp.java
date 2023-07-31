package com.tencent.supersonic.semantic.api.model.response;


import com.tencent.supersonic.common.pojo.QueryAuthorization;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.semantic.api.model.pojo.QueryResult;
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
