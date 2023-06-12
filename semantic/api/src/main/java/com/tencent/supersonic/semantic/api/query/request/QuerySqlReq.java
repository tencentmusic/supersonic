package com.tencent.supersonic.semantic.api.query.request;

import java.util.Map;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class QuerySqlReq {

    private Long domainId;

    private String sql;

    private Map<String, String> variables;

}
