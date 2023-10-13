package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectFunctionHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;

@Slf4j
public class GlobalAfterCorrector extends BaseSemanticCorrector {

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {

        super.correct(semanticCorrectInfo);
        String sql = semanticCorrectInfo.getSql();
        if (!SqlParserSelectFunctionHelper.hasAggregateFunction(sql)) {
            return;
        }
        Expression havingExpression = SqlParserSelectHelper.getHavingExpression(sql);
        if (Objects.nonNull(havingExpression)) {
            String replaceSql = SqlParserAddHelper.addFunctionToSelect(sql, havingExpression);
            semanticCorrectInfo.setSql(replaceSql);
        }
        return;
    }

}