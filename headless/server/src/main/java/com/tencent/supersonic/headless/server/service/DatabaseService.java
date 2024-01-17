package com.tencent.supersonic.headless.server.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.request.DatabaseReq;
import com.tencent.supersonic.headless.api.response.DatabaseResp;
import com.tencent.supersonic.headless.api.response.QueryResultWithSchemaResp;
import com.tencent.supersonic.headless.server.pojo.DatabaseParameter;
import java.util.List;
import java.util.Map;


public interface DatabaseService {

    QueryResultWithSchemaResp executeSql(String sql, DatabaseResp databaseResp);

    QueryResultWithSchemaResp executeSql(String sql, Long id, User user);

    Map<String, List<DatabaseParameter>> getDatabaseParameters();

    boolean testConnect(DatabaseReq databaseReq, User user);

    DatabaseResp createOrUpdateDatabase(DatabaseReq databaseReq, User user);

    List<DatabaseResp> getDatabaseList(User user);

    void deleteDatabase(Long databaseId);

    DatabaseResp getDatabase(Long id);

    QueryResultWithSchemaResp getDbNames(Long id);

    QueryResultWithSchemaResp getTables(Long id, String db);

    QueryResultWithSchemaResp getColumns(Long id, String db, String table);
}
