package com.tencent.supersonic.semantic.query.optimizer;

import com.google.common.base.Strings;
import com.tencent.supersonic.semantic.api.query.request.QueryStructReq;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component("DetailQuery")
public class DetailQuery implements QueryOptimizer {

    @Override
    public void rewrite(QueryStructReq queryStructCmd, QueryStatement queryStatement) {
        String sqlRaw = queryStatement.getSql().trim();
        if (Strings.isNullOrEmpty(sqlRaw)) {
            throw new RuntimeException("sql is empty or null");
        }
        log.debug("before handleNoMetric, sql:{}", sqlRaw);
        if (isDetailQuery(queryStructCmd)) {
            if (queryStructCmd.getMetrics().size() == 0 && !CollectionUtils.isEmpty(queryStructCmd.getGroups())) {
                String sqlForm = "select %s from ( %s ) src_no_metric";
                String sql = String.format(sqlForm, queryStructCmd.getGroups().stream().collect(
                        Collectors.joining(",")), sqlRaw);
                queryStatement.setSql(sql);
            }
        }
        log.debug("after handleNoMetric, sql:{}", queryStatement.getSql());
    }

    public boolean isDetailQuery(QueryStructReq queryStructCmd) {
        return Objects.nonNull(queryStructCmd) && queryStructCmd.getNativeQuery() && CollectionUtils.isEmpty(
                queryStructCmd.getMetrics());
    }
}
