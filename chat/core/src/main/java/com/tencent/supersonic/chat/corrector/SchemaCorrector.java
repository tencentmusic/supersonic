package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.SqlInfo;
import com.tencent.supersonic.chat.parser.llm.s2sql.ParseResult;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq;
import com.tencent.supersonic.chat.query.llm.s2sql.LLMReq.ElementValue;
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
public class SchemaCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryReq queryReq, SemanticParseInfo semanticParseInfo) {

        replaceAlias(semanticParseInfo);

        updateFieldNameByLinkingValue(semanticParseInfo);

        updateFieldValueByLinkingValue(semanticParseInfo);

        correctFieldName(semanticParseInfo);
    }

    private void replaceAlias(SemanticParseInfo semanticParseInfo) {
        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();
        String replaceAlias = SqlParserReplaceHelper.replaceAlias(sqlInfo.getCorrectS2SQL());
        sqlInfo.setCorrectS2SQL(replaceAlias);
    }

    private void correctFieldName(SemanticParseInfo semanticParseInfo) {
        Map<String, String> fieldNameMap = getFieldNameMap(semanticParseInfo.getModelId());
        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();
        String sql = SqlParserReplaceHelper.replaceFields(sqlInfo.getCorrectS2SQL(), fieldNameMap);
        sqlInfo.setCorrectS2SQL(sql);
    }

    private void updateFieldNameByLinkingValue(SemanticParseInfo semanticParseInfo) {
        List<ElementValue> linking = getLinkingValues(semanticParseInfo);
        if (CollectionUtils.isEmpty(linking)) {
            return;
        }

        Map<String, Set<String>> fieldValueToFieldNames = linking.stream().collect(
                Collectors.groupingBy(ElementValue::getFieldValue,
                        Collectors.mapping(ElementValue::getFieldName, Collectors.toSet())));

        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();

        String sql = SqlParserReplaceHelper.replaceFieldNameByValue(sqlInfo.getCorrectS2SQL(), fieldValueToFieldNames);
        sqlInfo.setCorrectS2SQL(sql);
    }

    private List<ElementValue> getLinkingValues(SemanticParseInfo semanticParseInfo) {
        Object context = semanticParseInfo.getProperties().get(Constants.CONTEXT);
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


    private void updateFieldValueByLinkingValue(SemanticParseInfo semanticParseInfo) {
        List<ElementValue> linking = getLinkingValues(semanticParseInfo);
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

        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();
        String sql = SqlParserReplaceHelper.replaceValue(sqlInfo.getCorrectS2SQL(), filedNameToValueMap, false);
        sqlInfo.setCorrectS2SQL(sql);
    }
}
