package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class SelectCorrector extends BaseSemanticCorrector {

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {
        super.correct(semanticCorrectInfo);
        String sql = semanticCorrectInfo.getSql();
        List<String> aggregateFields = SqlParserSelectHelper.getAggregateFields(sql);
        List<String> selectFields = SqlParserSelectHelper.getSelectFields(sql);
        // If the number of aggregated fields is equal to the number of queried fields, do not add fields to select.
        if (!CollectionUtils.isEmpty(aggregateFields)
                && !CollectionUtils.isEmpty(selectFields)
                && aggregateFields.size() == selectFields.size()) {
            return;
        }
        addFieldsToSelect(semanticCorrectInfo, sql);
    }
}
