package com.tencent.supersonic.chat.core.corrector;

import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * Perform SQL corrections on the "Select" section in S2SQL.
 */
@Slf4j
public class SelectCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectS2SQL();
        List<String> aggregateFields = SqlParserSelectHelper.getAggregateFields(correctS2SQL);
        List<String> selectFields = SqlParserSelectHelper.getSelectFields(correctS2SQL);
        // If the number of aggregated fields is equal to the number of queried fields, do not add fields to select.
        if (!CollectionUtils.isEmpty(aggregateFields)
                && !CollectionUtils.isEmpty(selectFields)
                && aggregateFields.size() == selectFields.size()) {
            return;
        }
        addFieldsToSelect(semanticParseInfo, correctS2SQL);
    }
}
