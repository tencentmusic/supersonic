package com.tencent.supersonic.headless.model.domain;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.common.model.request.DatabaseReq;
import com.tencent.supersonic.headless.common.model.response.DatabaseResp;
import com.tencent.supersonic.headless.common.model.response.QueryResultWithSchemaResp;
import java.util.List;


public interface DatabaseService {

    QueryResultWithSchemaResp executeSql(String sql, DatabaseResp databaseResp);

    QueryResultWithSchemaResp executeSql(String sql, Long id, User user);

    boolean testConnect(DatabaseReq databaseReq, User user);

    DatabaseResp createOrUpdateDatabase(DatabaseReq databaseReq, User user);

    List<DatabaseResp> getDatabaseList(User user);

    void deleteDatabase(Long databaseId);

    DatabaseResp getDatabase(Long id);

    QueryResultWithSchemaResp getDbNames(Long id);

    QueryResultWithSchemaResp getTables(Long id, String db);

    QueryResultWithSchemaResp getColumns(Long id, String db, String table);
}
