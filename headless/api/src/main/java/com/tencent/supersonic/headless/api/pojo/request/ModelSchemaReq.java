package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;

@Data
public class ModelSchemaReq {

    private Long databaseId;

    private String sql;

    private String db;

    private String table;

    private boolean buildByLLM;
}
