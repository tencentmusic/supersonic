package com.tencent.supersonic.headless.api.pojo.request;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import java.util.List;
import lombok.Data;

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
