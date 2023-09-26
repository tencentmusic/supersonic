package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import org.springframework.util.CollectionUtils;

@Slf4j
public class SelectCorrector extends BaseSemanticCorrector {

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {
        super.correct(semanticCorrectInfo);
        String sql = semanticCorrectInfo.getSql();

        if (SqlParserSelectHelper.hasAggregateFunction(sql)) {
            Expression havingExpression = SqlParserSelectHelper.getHavingExpression(sql);
            if (Objects.nonNull(havingExpression)) {
                String replaceSql = SqlParserUpdateHelper.addFunctionToSelect(sql, havingExpression);
                semanticCorrectInfo.setSql(replaceSql);
            }
            return;
        }
        Set<String> selectFields = new HashSet<>(SqlParserSelectHelper.getSelectFields(sql));
        Set<String> whereFields = new HashSet<>(SqlParserSelectHelper.getWhereFields(sql));

        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(whereFields)) {
            return;
        }

        whereFields.addAll(SqlParserSelectHelper.getOrderByFields(sql));
        whereFields.removeAll(selectFields);
        whereFields.remove(TimeDimensionEnum.DAY.getName());
        whereFields.remove(TimeDimensionEnum.WEEK.getName());
        whereFields.remove(TimeDimensionEnum.MONTH.getName());
        String replaceFields = SqlParserUpdateHelper.addFieldsToSelect(sql, new ArrayList<>(whereFields));
        semanticCorrectInfo.setSql(replaceFields);
    }
}
