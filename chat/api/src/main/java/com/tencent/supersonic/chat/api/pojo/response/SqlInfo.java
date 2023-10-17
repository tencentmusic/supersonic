package com.tencent.supersonic.chat.api.pojo.response;

import lombok.Data;

@Data
public class SqlInfo {

    private String s2QL;
    private String logicSql;
    private String querySql;
}
