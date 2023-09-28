package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;

@Slf4j
public class GlobalAfterCorrector extends BaseSemanticCorrector {

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {

        super.correct(semanticCorrectInfo);
        String sql = semanticCorrectInfo.getSql();
        if (!SqlParserSelectHelper.hasAggregateFunction(sql)) {
            return;
        }
        Expression havingExpression = SqlParserSelectHelper.getHavingExpression(sql);
        if (Objects.nonNull(havingExpression)) {
            String replaceSql = SqlParserUpdateHelper.addFunctionToSelect(sql, havingExpression);
            semanticCorrectInfo.setSql(replaceSql);
        }
        return;
    }

}