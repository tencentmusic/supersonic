package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
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
    public CorrectionInfo corrector(CorrectionInfo correctionInfo) {
        String sql = correctionInfo.getSql();
        if (SqlParserSelectHelper.hasAggregateFunction(sql)) {
            return correctionInfo;
        }
        Set<String> selectFields = new HashSet<>(SqlParserSelectHelper.getSelectFields(sql));
        Set<String> whereFields = new HashSet<>(SqlParserSelectHelper.getWhereFields(sql));

        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(whereFields)) {
            return correctionInfo;
        }

        whereFields.addAll(SqlParserSelectHelper.getOrderByFields(sql));
        whereFields.removeAll(selectFields);
        whereFields.remove(TimeDimensionEnum.DAY.getName());
        whereFields.remove(TimeDimensionEnum.WEEK.getName());
        whereFields.remove(TimeDimensionEnum.MONTH.getName());

        String replaceFields = SqlParserUpdateHelper.addFieldsToSelect(sql, new ArrayList<>(whereFields));
        correctionInfo.setSql(replaceFields);
        return correctionInfo;
    }
}
