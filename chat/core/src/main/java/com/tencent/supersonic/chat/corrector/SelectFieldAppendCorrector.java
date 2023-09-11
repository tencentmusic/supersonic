package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class SelectFieldAppendCorrector extends BaseSemanticCorrector {

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {
        String preSql = semanticCorrectInfo.getSql();
        if (SqlParserSelectHelper.hasAggregateFunction(preSql)) {
            return;
        }
        Set<String> selectFields = new HashSet<>(SqlParserSelectHelper.getSelectFields(preSql));
        Set<String> whereFields = new HashSet<>(SqlParserSelectHelper.getWhereFields(preSql));

        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(whereFields)) {
            return;
        }

        whereFields.addAll(SqlParserSelectHelper.getOrderByFields(preSql));
        whereFields.removeAll(selectFields);
        whereFields.remove(TimeDimensionEnum.DAY.getName());
        whereFields.remove(TimeDimensionEnum.WEEK.getName());
        whereFields.remove(TimeDimensionEnum.MONTH.getName());
        String replaceFields = SqlParserUpdateHelper.addFieldsToSelect(preSql, new ArrayList<>(whereFields));
        semanticCorrectInfo.setPreSql(preSql);
        semanticCorrectInfo.setSql(replaceFields);
    }
}
