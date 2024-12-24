package com.tencent.supersonic.headless.core.adaptor.db;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HanadbAdaptor extends DefaultDbAdaptor {

    @Override
    public String rewriteSql(String sql) {
        return sql.replaceAll("`(.*?)`", "\"$1\"").replaceAll("\"([A-Z0-9_]+?)\"", "$1");
    }

}
