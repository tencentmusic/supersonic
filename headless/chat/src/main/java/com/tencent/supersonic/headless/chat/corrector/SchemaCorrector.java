package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.jsqlparser.*;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.util.DateUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.parser.llm.ParseResult;
import com.tencent.supersonic.headless.chat.query.llm.s2sql.LLMReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/** Perform schema corrections on the Schema information in S2SQL. */
@Slf4j
public class SchemaCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {

        removeDateFields(chatQueryContext, semanticParseInfo);

        correctAggFunction(semanticParseInfo);

        updateFieldNameByLinkingValue(semanticParseInfo);

        updateFieldValueByLinkingValue(semanticParseInfo);

        correctFieldName(chatQueryContext, semanticParseInfo);
    }

    private void removeDateFields(ChatQueryContext chatQueryContext,
            SemanticParseInfo semanticParseInfo) {
        if (containsPartitionDimensions(chatQueryContext, semanticParseInfo)) {
            return;
        }
        removeDateIfExist(chatQueryContext, semanticParseInfo);
    }

    private void correctAggFunction(SemanticParseInfo semanticParseInfo) {
        Map<String, String> aggregateEnum = AggregateEnum.getAggregateEnum();
        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();
        String sql = SqlReplaceHelper.replaceFunction(sqlInfo.getCorrectedS2SQL(), aggregateEnum);
        sqlInfo.setCorrectedS2SQL(sql);
    }

    private void correctFieldName(ChatQueryContext chatQueryContext,
            SemanticParseInfo semanticParseInfo) {
        Map<String, String> fieldNameMap =
                getFieldNameMap(chatQueryContext, semanticParseInfo.getDataSetId());
        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();
        String sql = SqlReplaceHelper.replaceFields(sqlInfo.getCorrectedS2SQL(), fieldNameMap);
        sqlInfo.setCorrectedS2SQL(sql);
    }

    private void updateFieldNameByLinkingValue(SemanticParseInfo semanticParseInfo) {
        List<LLMReq.ElementValue> linking = getLinkingValues(semanticParseInfo);
        if (CollectionUtils.isEmpty(linking)) {
            return;
        }

        Map<String, Set<String>> fieldValueToFieldNames =
                linking.stream().collect(Collectors.groupingBy(LLMReq.ElementValue::getFieldValue,
                        Collectors.mapping(LLMReq.ElementValue::getFieldName, Collectors.toSet())));

        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();

        String sql = SqlReplaceHelper.replaceFieldNameByValue(sqlInfo.getCorrectedS2SQL(),
                fieldValueToFieldNames);
        sqlInfo.setCorrectedS2SQL(sql);
    }

    private List<LLMReq.ElementValue> getLinkingValues(SemanticParseInfo semanticParseInfo) {
        Object context = semanticParseInfo.getProperties().get(Constants.CONTEXT);
        if (Objects.isNull(context)) {
            return null;
        }

        ParseResult parseResult = JsonUtil.toObject(JsonUtil.toString(context), ParseResult.class);
        if (Objects.isNull(parseResult) || Objects.isNull(parseResult.getLlmReq())) {
            return null;
        }
        return parseResult.getLlmReq().getSchema().getValues();
    }

    private void updateFieldValueByLinkingValue(SemanticParseInfo semanticParseInfo) {
        List<LLMReq.ElementValue> linking = getLinkingValues(semanticParseInfo);
        if (CollectionUtils.isEmpty(linking)) {
            return;
        }

        Map<String, Map<String, String>> filedNameToValueMap = linking.stream()
                .collect(Collectors.groupingBy(LLMReq.ElementValue::getFieldName,
                        Collectors.mapping(LLMReq.ElementValue::getFieldValue,
                                Collectors.toMap(oldValue -> oldValue, newValue -> newValue,
                                        (existingValue, newValue) -> newValue))));

        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();
        String sql = SqlReplaceHelper.replaceValue(sqlInfo.getCorrectedS2SQL(), filedNameToValueMap,
                false);
        sqlInfo.setCorrectedS2SQL(sql);
    }

    public void removeUnmappedFilterValue(ChatQueryContext chatQueryContext,
            SemanticParseInfo semanticParseInfo) {
        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();
        String correctS2SQL = sqlInfo.getCorrectedS2SQL();
        List<FieldExpression> whereExpressionList =
                SqlSelectHelper.getWhereExpressions(correctS2SQL);
        if (CollectionUtils.isEmpty(whereExpressionList)) {
            return;
        }
        List<LLMReq.ElementValue> linkingValues = getLinkingValues(semanticParseInfo);
        SemanticSchema semanticSchema = chatQueryContext.getSemanticSchema();
        Set<String> dimensions = getDimensions(semanticParseInfo.getDataSetId(), semanticSchema);

        if (CollectionUtils.isEmpty(linkingValues)) {
            linkingValues = new ArrayList<>();
        }
        Set<String> linkingFieldNames = linkingValues.stream()
                .map(linking -> linking.getFieldName()).collect(Collectors.toSet());

        Set<String> removeFieldNames = whereExpressionList.stream()
                .filter(fieldExpression -> StringUtils.isBlank(fieldExpression.getFunction()))
                .filter(fieldExpression -> FilterOperatorEnum.EQUALS.getValue()
                        .equals(fieldExpression.getOperator()))
                .filter(fieldExpression -> dimensions.contains(fieldExpression.getFieldName()))
                .filter(fieldExpression -> !DateUtils
                        .isAnyDateString(fieldExpression.getFieldValue().toString()))
                .filter(fieldExpression -> !linkingFieldNames
                        .contains(fieldExpression.getFieldName()))
                .map(fieldExpression -> fieldExpression.getFieldName()).collect(Collectors.toSet());

        String sql = SqlRemoveHelper.removeWhereCondition(correctS2SQL, removeFieldNames);
        sqlInfo.setCorrectedS2SQL(sql);
    }
}
