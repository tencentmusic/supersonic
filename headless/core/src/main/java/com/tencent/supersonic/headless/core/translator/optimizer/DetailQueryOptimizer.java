package com.tencent.supersonic.headless.core.translator.optimizer;

import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import com.tencent.supersonic.headless.core.pojo.StructQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Remove the default metric added by the system when the query only has dimensions */
@Slf4j
@Component("DetailQueryOptimizer")
public class DetailQueryOptimizer implements QueryOptimizer {

    @Override
    public void rewrite(QueryStatement queryStatement) {
        StructQuery structQuery = queryStatement.getStructQuery();
        String sqlRaw = queryStatement.getSql().trim();
        if (StringUtils.isEmpty(sqlRaw)) {
            throw new RuntimeException("sql is empty or null");
        }
        log.debug("before handleNoMetric, sql:{}", sqlRaw);
        // if (isDetailQuery(structQueryParam)) {
        // if (!CollectionUtils.isEmpty(structQueryParam.getGroups())) {
        // String sqlForm = "select %s from ( %s ) src_no_metric";
        // String sql = String.format(sqlForm,
        // structQueryParam.getGroups().stream().collect(Collectors.joining(",")),
        // sqlRaw);
        // queryStatement.setSql(sql);
        // }
        // }
        log.debug("after handleNoMetric, sql:{}", queryStatement.getSql());
    }

    public boolean isDetailQuery(StructQuery structQuery) {
        return Objects.nonNull(structQuery) && structQuery.getQueryType().isNativeAggQuery();
    }
}
