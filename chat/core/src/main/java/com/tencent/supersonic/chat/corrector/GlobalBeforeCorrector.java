package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.parser.llm.s2ql.ParseResult;
import com.tencent.supersonic.chat.query.llm.s2ql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2ql.LLMReq.ElementValue;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserReplaceHelper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class GlobalBeforeCorrector extends BaseSemanticCorrector {

    @Override
    public void correct(SemanticCorrectInfo semanticCorrectInfo) {

        super.correct(semanticCorrectInfo);

        replaceAlias(semanticCorrectInfo);

        updateFieldNameByLinkingValue(semanticCorrectInfo);

        updateFieldValueByLinkingValue(semanticCorrectInfo);

        correctFieldName(semanticCorrectInfo);
    }

    private void replaceAlias(SemanticCorrectInfo semanticCorrectInfo) {
        String replaceAlias = SqlParserReplaceHelper.replaceAlias(semanticCorrectInfo.getSql());
        semanticCorrectInfo.setSql(replaceAlias);
    }

    private void correctFieldName(SemanticCorrectInfo semanticCorrectInfo) {

        Map<String, String> fieldNameMap = getFieldNameMap(semanticCorrectInfo.getParseInfo().getModelId());

        String sql = SqlParserReplaceHelper.replaceFields(semanticCorrectInfo.getSql(), fieldNameMap);

        semanticCorrectInfo.setSql(sql);
    }

    private void updateFieldNameByLinkingValue(SemanticCorrectInfo semanticCorrectInfo) {
        List<ElementValue> linking = getLinkingValues(semanticCorrectInfo);
        if (CollectionUtils.isEmpty(linking)) {
            return;
        }

        Map<String, Set<String>> fieldValueToFieldNames = linking.stream().collect(
                Collectors.groupingBy(ElementValue::getFieldValue,
                        Collectors.mapping(ElementValue::getFieldName, Collectors.toSet())));

        String sql = SqlParserReplaceHelper.replaceFieldNameByValue(semanticCorrectInfo.getSql(),
                fieldValueToFieldNames);
        semanticCorrectInfo.setSql(sql);
    }

    private List<ElementValue> getLinkingValues(SemanticCorrectInfo semanticCorrectInfo) {
        Object context = semanticCorrectInfo.getParseInfo().getProperties().get(Constants.CONTEXT);
        if (Objects.isNull(context)) {
            return null;
        }

        ParseResult parseResult = JsonUtil.toObject(JsonUtil.toString(context), ParseResult.class);
        if (Objects.isNull(parseResult) || Objects.isNull(parseResult.getLlmReq())) {
            return null;
        }
        LLMReq llmReq = parseResult.getLlmReq();
        return llmReq.getLinking();
    }


    private void updateFieldValueByLinkingValue(SemanticCorrectInfo semanticCorrectInfo) {
        List<ElementValue> linking = getLinkingValues(semanticCorrectInfo);
        if (CollectionUtils.isEmpty(linking)) {
            return;
        }

        Map<String, Map<String, String>> filedNameToValueMap = linking.stream().collect(
                Collectors.groupingBy(ElementValue::getFieldName,
                        Collectors.mapping(ElementValue::getFieldValue, Collectors.toMap(
                                oldValue -> oldValue,
                                newValue -> newValue,
                                (existingValue, newValue) -> newValue)
                        )));

        String sql = SqlParserReplaceHelper.replaceValue(semanticCorrectInfo.getSql(), filedNameToValueMap, false);
        semanticCorrectInfo.setSql(sql);
    }
}