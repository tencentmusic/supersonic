package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import lombok.Data;

import java.util.List;

@Data
public class ModelSchemaReq {

    private Long databaseId;

    private String sql;

    private String db;

    private List<String> tables;

    private boolean buildByLLM;

    private Integer chatModelId;

    private ChatModelConfig chatModelConfig;
}
