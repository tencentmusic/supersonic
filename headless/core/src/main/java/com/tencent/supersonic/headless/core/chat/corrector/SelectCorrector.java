package com.tencent.supersonic.headless.core.chat.corrector;


import com.tencent.supersonic.common.util.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Perform SQL corrections on the "Select" section in S2SQL.
 */
@Slf4j
public class SelectCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectS2SQL();
        List<String> aggregateFields = SqlSelectHelper.getAggregateFields(correctS2SQL);
        List<String> selectFields = SqlSelectHelper.getSelectFields(correctS2SQL);
        // If the number of aggregated fields is equal to the number of queried fields, do not add fields to select.
        if (!CollectionUtils.isEmpty(aggregateFields)
                && !CollectionUtils.isEmpty(selectFields)
                && aggregateFields.size() == selectFields.size()) {
            return;
        }
        correctS2SQL = addFieldsToSelect(queryContext, semanticParseInfo, correctS2SQL);
        String querySql = SqlReplaceHelper.dealAliasToOrderBy(correctS2SQL);
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(querySql);
    }
}
