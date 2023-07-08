package com.tencent.supersonic.chat.api.response;

import com.tencent.supersonic.chat.api.pojo.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.semantic.api.core.pojo.QueryAuthorization;
import com.tencent.supersonic.semantic.api.core.pojo.QueryColumn;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class QueryResultResp {

    public EntityInfo entityInfo;
    private Long queryId;
    private String queryMode;
    private String querySql;
    private int queryState;
    private List<QueryColumn> queryColumns;
    private QueryAuthorization queryAuthorization;
    private SemanticParseInfo chatContext;
    private Object response;
    private List<Map<String, Object>> queryResults;
}
