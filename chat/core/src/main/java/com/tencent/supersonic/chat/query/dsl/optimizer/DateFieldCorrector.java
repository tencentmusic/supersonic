package com.tencent.supersonic.chat.query.dsl.optimizer;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
import com.tencent.supersonic.chat.parser.llm.dsl.DSLDateHelper;
import com.tencent.supersonic.common.util.jsqlparser.CCJSqlParserUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class DateFieldCorrector extends BaseDSLOptimizer {

    @Override
    public CorrectionInfo rewriter(CorrectionInfo correctionInfo) {

        String sql = correctionInfo.getSql();
        List<String> whereFields = CCJSqlParserUtils.getWhereFields(sql);
        if (CollectionUtils.isEmpty(whereFields) || !whereFields.contains(BaseDSLOptimizer.DATE_FIELD)) {
            String currentDate = DSLDateHelper.getCurrentDate(correctionInfo.getParseInfo().getModelId());
            sql = CCJSqlParserUtils.addWhere(sql, BaseDSLOptimizer.DATE_FIELD, currentDate);
        }
        correctionInfo.setSql(sql);
        return correctionInfo;
    }

}
