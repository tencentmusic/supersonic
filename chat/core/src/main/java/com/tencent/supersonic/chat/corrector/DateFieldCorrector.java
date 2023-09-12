package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.parser.llm.dsl.DSLDateHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class DateFieldCorrector extends BaseSemanticCorrector {

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {

        String sql = semanticCorrectInfo.getSql();
        List<String> whereFields = SqlParserSelectHelper.getWhereFields(sql);
        if (CollectionUtils.isEmpty(whereFields) || !whereFields.contains(DATE_FIELD)) {
            String currentDate = DSLDateHelper.getReferenceDate(semanticCorrectInfo.getParseInfo().getModelId());
            sql = SqlParserUpdateHelper.addWhere(sql, DATE_FIELD, currentDate);
        }
        semanticCorrectInfo.setPreSql(semanticCorrectInfo.getSql());
        semanticCorrectInfo.setSql(sql);
    }

}
