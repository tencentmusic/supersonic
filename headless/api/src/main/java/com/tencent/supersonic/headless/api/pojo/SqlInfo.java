package com.tencent.supersonic.headless.api.pojo;

import lombok.Data;

@Data
public class SqlInfo {

    private String s2SQL;
    private String correctS2SQL;
    private String querySQL;
    private String sourceId;
}
