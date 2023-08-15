package com.tencent.supersonic.semantic.model.domain;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.model.request.DatabaseReq;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.SqlParserResp;


public interface DatabaseService {

    QueryResultWithSchemaResp executeSql(String sql, DatabaseResp databaseResp);

    DatabaseResp getDatabaseByModelId(Long modelId);

    QueryResultWithSchemaResp executeSql(String sql, Long domainId);

    boolean testConnect(DatabaseReq databaseReq, User user);

    DatabaseResp createOrUpdateDatabase(DatabaseReq databaseReq, User user);

    DatabaseResp getDatabase(Long id);

    // one domain only has one database
    DatabaseResp getDatabaseByDomainId(Long domainId);

    QueryResultWithSchemaResp queryWithColumns(SqlParserResp sqlParser);

    QueryResultWithSchemaResp getDbNames(Long id);

    QueryResultWithSchemaResp getTables(Long id, String db);

    QueryResultWithSchemaResp getColumns(Long id, String db, String table);
}
