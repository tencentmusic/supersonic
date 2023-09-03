package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.CorrectionInfo;
import com.tencent.supersonic.chat.parser.llm.dsl.DSLParseResult;
import com.tencent.supersonic.chat.query.llm.dsl.LLMReq;
import com.tencent.supersonic.chat.query.llm.dsl.LLMReq.ElementValue;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class FieldNameCorrector extends BaseSemanticCorrector {

    @Override
    public CorrectionInfo corrector(CorrectionInfo correctionInfo) {

        Object context = correctionInfo.getParseInfo().getProperties().get(Constants.CONTEXT);
        if (Objects.isNull(context)) {
            return correctionInfo;
        }

        DSLParseResult dslParseResult = JsonUtil.toObject(JsonUtil.toString(context), DSLParseResult.class);
        if (Objects.isNull(dslParseResult) || Objects.isNull(dslParseResult.getLlmReq())) {
            return correctionInfo;
        }
        LLMReq llmReq = dslParseResult.getLlmReq();
        List<ElementValue> linking = llmReq.getLinking();
        if (CollectionUtils.isEmpty(linking)) {
            return correctionInfo;
        }

        Map<String, Set<String>> fieldValueToFieldNames = linking.stream().collect(
                Collectors.groupingBy(ElementValue::getFieldValue,
                        Collectors.mapping(ElementValue::getFieldName, Collectors.toSet())));

        String preSql = correctionInfo.getSql();
        correctionInfo.setPreSql(preSql);
        String sql = SqlParserUpdateHelper.replaceFieldNameByValue(preSql, fieldValueToFieldNames);
        correctionInfo.setSql(sql);
        return correctionInfo;
    }

}
