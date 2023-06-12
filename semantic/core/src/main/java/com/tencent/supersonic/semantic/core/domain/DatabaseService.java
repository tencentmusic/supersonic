package com.tencent.supersonic.semantic.core.domain;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.core.request.DatabaseReq;
import com.tencent.supersonic.semantic.api.core.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.core.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.SqlParserResp;


public interface DatabaseService {

    QueryResultWithSchemaResp executeSql(String sql, DatabaseResp databaseResp);

    QueryResultWithSchemaResp executeSql(String sql, Long domainId);

    boolean testConnect(DatabaseReq databaseReq, User user);

    DatabaseResp createOrUpdateDatabase(DatabaseReq databaseReq, User user);

    DatabaseResp getDatabase(Long id);

    // one domain only has one database
    DatabaseResp getDatabaseByDomainId(Long domainId);

    QueryResultWithSchemaResp queryWithColumns(SqlParserResp sqlParser);

}
