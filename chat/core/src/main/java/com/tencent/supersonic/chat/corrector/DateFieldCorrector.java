package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
import com.tencent.supersonic.chat.parser.llm.dsl.DSLDateHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class DateFieldCorrector extends BaseSemanticCorrector {

    @Override
    public CorrectionInfo corrector(CorrectionInfo correctionInfo) {

        String sql = correctionInfo.getSql();
        List<String> whereFields = SqlParserSelectHelper.getWhereFields(sql);
        if (CollectionUtils.isEmpty(whereFields) || !whereFields.contains(DATE_FIELD)) {
            String currentDate = DSLDateHelper.getCurrentDate(correctionInfo.getParseInfo().getModelId());
            sql = SqlParserUpdateHelper.addWhere(sql, DATE_FIELD, currentDate);
        }
        correctionInfo.setPreSql(correctionInfo.getSql());
        correctionInfo.setSql(sql);
        return correctionInfo;
    }

}
