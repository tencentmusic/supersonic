package com.tencent.supersonic.headless.core.pojo;

import lombok.Data;

@Data
public class SqlQuery {
    private String sql;
    private String table;
    private boolean supportWith = true;
    private boolean withAlias = true;
    private String simplifiedSql;
}
