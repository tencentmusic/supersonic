package com.tencent.supersonic.headless.integration;

import com.tencent.supersonic.BaseApplication;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.QuerySqlReq;
import com.tencent.supersonic.headless.api.pojo.request.SemanticQueryReq;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.service.QueryService;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

public class BaseTest extends BaseApplication {

    @Autowired
    private QueryService queryService;

    protected SemanticQueryResp queryBySql(String sql) throws Exception {
        return queryBySql(sql, User.getFakeUser());
    }

    protected SemanticQueryResp queryBySql(String sql, User user) throws Exception {
        return queryService.queryByReq(buildQuerySqlReq(sql), user);
    }

    protected SemanticQueryResp queryByReq(SemanticQueryReq queryReq, User user) throws Exception {
        return queryService.queryByReq(queryReq, user);
    }

    protected SemanticQueryReq buildQuerySqlReq(String sql) {
        QuerySqlReq querySqlCmd = new QuerySqlReq();
        querySqlCmd.setSql(sql);
        Set<Long> modelIds = new HashSet<>();
        modelIds.add(1L);
        modelIds.add(2L);
        modelIds.add(3L);
        querySqlCmd.setModelIds(modelIds);
        return querySqlCmd;
    }

}
