package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

@Data
public class SqlInfo {

    private String llmParseSql;
    private String logicSql;
    private String querySql;
}
