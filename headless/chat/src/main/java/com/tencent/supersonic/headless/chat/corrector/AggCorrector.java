package com.tencent.supersonic.headless.chat.corrector;


import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.QueryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Verify whether the SQL aggregate function is missing. If it is missing, fill it in.
 */
@Slf4j
public class AggCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        addAggregate(queryContext, semanticParseInfo);
    }

    private void addAggregate(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        List<String> sqlGroupByFields = SqlSelectHelper.getGroupByFields(
                semanticParseInfo.getSqlInfo().getCorrectS2SQL());
        if (CollectionUtils.isEmpty(sqlGroupByFields)) {
            return;
        }
        addAggregateToMetric(queryContext, semanticParseInfo);
    }

}
