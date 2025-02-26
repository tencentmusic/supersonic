package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.headless.api.pojo.DbSchema;
import lombok.Data;

import java.util.List;

@Data
public class ModelBuildReq {

    private String name;

    private String bizName;

    private Long databaseId;

    private Long domainId;

    private String sql;

    private String catalog;

    private String db;

    private List<String> tables;

    private List<DbSchema> dbSchemas;

    private boolean buildByLLM;

    private Integer chatModelId;

    private ChatModelConfig chatModelConfig;
}
