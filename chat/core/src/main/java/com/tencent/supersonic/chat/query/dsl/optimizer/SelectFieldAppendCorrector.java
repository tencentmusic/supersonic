package com.tencent.supersonic.chat.query.dsl.optimizer;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
import com.tencent.supersonic.common.util.jsqlparser.CCJSqlParserUtils;
import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class SelectFieldAppendCorrector extends BaseDSLOptimizer {

    @Override
    public CorrectionInfo rewriter(CorrectionInfo correctionInfo) {
        String sql = correctionInfo.getSql();
        if (CCJSqlParserUtils.hasAggregateFunction(sql)) {
            return correctionInfo;
        }
        Set<String> selectFields = new HashSet<>(CCJSqlParserUtils.getSelectFields(sql));
        Set<String> whereFields = new HashSet<>(CCJSqlParserUtils.getWhereFields(sql));
        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(whereFields)) {
            return correctionInfo;
        }

        whereFields.removeAll(selectFields);
        whereFields.remove(TimeDimensionEnum.DAY.getName());
        whereFields.remove(TimeDimensionEnum.WEEK.getName());
        whereFields.remove(TimeDimensionEnum.MONTH.getName());
        String replaceFields = CCJSqlParserUtils.addFieldsToSelect(sql, new ArrayList<>(whereFields));
        correctionInfo.setSql(replaceFields);
        return correctionInfo;
    }
}
