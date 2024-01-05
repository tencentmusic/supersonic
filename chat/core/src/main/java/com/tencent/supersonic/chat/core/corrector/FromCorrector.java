package com.tencent.supersonic.chat.core.corrector;

import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserReplaceHelper;
import lombok.extern.slf4j.Slf4j;
/**
 * Perform SQL corrections on the "From" section in S2SQL.
 */
@Slf4j
public class FromCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        String modelName = semanticParseInfo.getModel().getName();
        String correctSql = SqlParserReplaceHelper
                .replaceTable(semanticParseInfo.getSqlInfo().getCorrectS2SQL(), modelName);
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(correctSql);
    }

}
