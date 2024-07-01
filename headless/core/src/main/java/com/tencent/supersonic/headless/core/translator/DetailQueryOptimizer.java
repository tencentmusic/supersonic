package com.tencent.supersonic.headless.core.translator;

import com.tencent.supersonic.headless.api.pojo.QueryParam;
import com.tencent.supersonic.headless.core.pojo.QueryStatement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Remove the default metric added by the system when the query only has dimensions
 */
@Slf4j
@Component("DetailQueryOptimizer")
public class DetailQueryOptimizer implements QueryOptimizer {

    @Override
    public void rewrite(QueryStatement queryStatement) {
        QueryParam queryParam = queryStatement.getQueryParam();
        String sqlRaw = queryStatement.getSql().trim();
        if (StringUtils.isEmpty(sqlRaw)) {
            throw new RuntimeException("sql is empty or null");
        }
        log.debug("before handleNoMetric, sql:{}", sqlRaw);
        if (isDetailQuery(queryParam)) {
            if (queryParam.getMetrics().size() == 0 && !CollectionUtils.isEmpty(queryParam.getGroups())) {
                String sqlForm = "select %s from ( %s ) src_no_metric";
                String sql = String.format(sqlForm, queryParam.getGroups().stream().collect(
                        Collectors.joining(",")), sqlRaw);
                queryStatement.setSql(sql);
            }
        }
        log.debug("after handleNoMetric, sql:{}", queryStatement.getSql());
    }

    public boolean isDetailQuery(QueryParam queryParam) {
        return Objects.nonNull(queryParam) && queryParam.getQueryType().isNativeAggQuery()
                && CollectionUtils.isEmpty(queryParam.getMetrics());
    }
}
