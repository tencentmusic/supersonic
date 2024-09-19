package com.tencent.supersonic.headless.api.pojo.request;

import lombok.Data;

@Data
public class ColumnReq {
    private Long databaseId;
    private String sql;
}
