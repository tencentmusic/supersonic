package com.tencent.supersonic.chat.server.userplugin.base.dao;

import net.hasor.dataql.fx.db.fxquery.DefaultFxQuery;
import net.hasor.dataql.fx.db.likemybatis.SqlNode;

import java.util.List;
import java.util.Map;

public class MybatisSqlQuery extends DefaultFxQuery {
    private SqlNode sqlNode;

    public MybatisSqlQuery(SqlNode sqlNode) {
        this.sqlNode = sqlNode;
    }

    public String buildQueryString(Object context) {
        if (context instanceof Map) {
            return this.sqlNode.getSql((Map) context);
        } else {
            throw new IllegalArgumentException("context must be instance of Map");
        }
    }

    public List<Object> buildParameterSource(Object context) {
        return this.sqlNode.getParameters();
    }
}
