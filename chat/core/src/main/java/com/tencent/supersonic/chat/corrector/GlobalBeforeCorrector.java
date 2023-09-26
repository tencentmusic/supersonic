package com.tencent.supersonic.chat.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticCorrectInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.parser.llm.dsl.DSLParseResult;
import com.tencent.supersonic.chat.query.llm.dsl.LLMReq;
import com.tencent.supersonic.chat.query.llm.dsl.LLMReq.ElementValue;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.AggregateTypeEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserUpdateHelper;
import com.tencent.supersonic.knowledge.service.SchemaService;
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

        updateFieldNameByBizName(semanticCorrectInfo);

        addAggregateToMetric(semanticCorrectInfo);
    }

    private void addAggregateToMetric(SemanticCorrectInfo semanticCorrectInfo) {
        //add aggregate to all metric
        String sql = semanticCorrectInfo.getSql();
        Long modelId = semanticCorrectInfo.getParseInfo().getModel().getModel();

        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();

        Map<String, String> metricToAggregate = semanticSchema.getMetrics(modelId).stream()
                .map(schemaElement -> {
                    if (Objects.isNull(schemaElement.getDefaultAgg())) {
                        schemaElement.setDefaultAgg(AggregateTypeEnum.SUM.name());
                    }
                    return schemaElement;
                }).collect(Collectors.toMap(a -> a.getBizName(), a -> a.getDefaultAgg(), (k1, k2) -> k1));

        if (CollectionUtils.isEmpty(metricToAggregate)) {
            return;
        }

        String aggregateSql = SqlParserUpdateHelper.addAggregateToField(sql, metricToAggregate);
        semanticCorrectInfo.setSql(aggregateSql);
    }

    private void replaceAlias(SemanticCorrectInfo semanticCorrectInfo) {
        String replaceAlias = SqlParserUpdateHelper.replaceAlias(semanticCorrectInfo.getSql());
        semanticCorrectInfo.setSql(replaceAlias);
    }

    private void updateFieldNameByBizName(SemanticCorrectInfo semanticCorrectInfo) {

        Map<String, String> fieldToBizName = getFieldToBizName(semanticCorrectInfo.getParseInfo().getModelId());

        String sql = SqlParserUpdateHelper.replaceFields(semanticCorrectInfo.getSql(), fieldToBizName);

        semanticCorrectInfo.setSql(sql);
    }

    private void updateFieldNameByLinkingValue(SemanticCorrectInfo semanticCorrectInfo) {
        Object context = semanticCorrectInfo.getParseInfo().getProperties().get(Constants.CONTEXT);
        if (Objects.isNull(context)) {
            return;
        }

        DSLParseResult dslParseResult = JsonUtil.toObject(JsonUtil.toString(context), DSLParseResult.class);
        if (Objects.isNull(dslParseResult) || Objects.isNull(dslParseResult.getLlmReq())) {
            return;
        }
        LLMReq llmReq = dslParseResult.getLlmReq();
        List<ElementValue> linking = llmReq.getLinking();
        if (CollectionUtils.isEmpty(linking)) {
            return;
        }

        Map<String, Set<String>> fieldValueToFieldNames = linking.stream().collect(
                Collectors.groupingBy(ElementValue::getFieldValue,
                        Collectors.mapping(ElementValue::getFieldName, Collectors.toSet())));

        String sql = SqlParserUpdateHelper.replaceFieldNameByValue(semanticCorrectInfo.getSql(),
                fieldValueToFieldNames);
        semanticCorrectInfo.setSql(sql);
    }
}