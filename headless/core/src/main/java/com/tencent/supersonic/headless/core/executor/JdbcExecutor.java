package com.tencent.supersonic.headless.core.executor;

import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.response.DatabaseResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import com.tencent.supersonic.headless.core.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("JdbcExecutor")
@Slf4j
public class JdbcExecutor implements QueryExecutor {
    @Override
    public boolean accept(QueryStatement queryStatement) {
        return true;
    }

    @Override
    public SemanticQueryResp execute(QueryStatement queryStatement) {
        // accelerate query if possible
        for (QueryAccelerator queryAccelerator : ComponentFactory.getQueryAccelerators()) {
            if (queryAccelerator.check(queryStatement)) {
                SemanticQueryResp semanticQueryResp = queryAccelerator.query(queryStatement);
                if (Objects.nonNull(semanticQueryResp)
                        && !semanticQueryResp.getResultList().isEmpty()) {
                    log.info("query by Accelerator {}",
                            queryAccelerator.getClass().getSimpleName());
                    return semanticQueryResp;
                }
            }
        }

        SqlUtils sqlUtils = ContextUtils.getBean(SqlUtils.class);
        String sql = StringUtils.normalizeSpace(queryStatement.getSql());
        log.info("executing SQL: {}", sql);
        DatabaseResp database = queryStatement.getOntology().getDatabase();
        SemanticQueryResp queryResultWithColumns = new SemanticQueryResp();
        try {
            SqlUtils sqlUtil = sqlUtils.init(database);
            sqlUtil.queryInternal(queryStatement.getSql(), queryResultWithColumns);
            queryResultWithColumns.setSql(sql);
        } catch (Exception e) {
            log.error("queryInternal with error ", e);
            queryResultWithColumns.setErrorMsg(e.getMessage());
        }
        return queryResultWithColumns;
    }
}
