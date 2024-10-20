package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import lombok.Data;

@Data
public class ModelSchemaReq {

    private Long databaseId;

    private String sql;

    private String db;

    private String table;

    private boolean buildByLLM;

    private ChatModelConfig chatModelConfig;
}
