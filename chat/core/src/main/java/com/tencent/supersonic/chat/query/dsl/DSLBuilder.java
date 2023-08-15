package com.tencent.supersonic.chat.query.dsl;

import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.StringUtil;
import com.tencent.supersonic.common.util.jsqlparser.CCJSqlParserUtils;
import com.tencent.supersonic.knowledge.service.SchemaService;
import com.tencent.supersonic.semantic.api.model.enums.TimeDimensionEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class DSLBuilder {

    public static final String DATA_Field = "数据日期";
    public static final String TABLE_PREFIX = "t_";

    public String build(SemanticParseInfo parseInfo, QueryFilters queryFilters, LLMResp llmResp, Long modelId)
            throws Exception {

        String sqlOutput = llmResp.getSqlOutput();
        SemanticSchema semanticSchema = ContextUtils.getBean(SchemaService.class).getSemanticSchema();
        List<SchemaElement> dbAllFields = new ArrayList<>();
        dbAllFields.addAll(semanticSchema.getMetrics());
        dbAllFields.addAll(semanticSchema.getDimensions());

        Map<String, String> fieldToBizName = getMapInfo(modelId, dbAllFields);
        fieldToBizName.put(DATA_Field, TimeDimensionEnum.DAY.getName());

        sqlOutput = CCJSqlParserUtils.replaceFields(sqlOutput, fieldToBizName);

        sqlOutput = CCJSqlParserUtils.replaceTable(sqlOutput, TABLE_PREFIX + modelId);

        String queryFilter = getQueryFilter(queryFilters);
        if (StringUtils.isNotEmpty(queryFilter)) {
            log.info("add queryFilter to sql :{}", queryFilter);
            Expression expression = CCJSqlParserUtil.parseCondExpression(queryFilter);
            CCJSqlParserUtils.addWhere(sqlOutput, expression);
        }

        log.info("build sqlOutput:{}", sqlOutput);
        return sqlOutput;
    }

    protected Map<String, String> getMapInfo(Long modelId, List<SchemaElement> metrics) {
        return metrics.stream().filter(entry -> entry.getModel().equals(modelId))
                .collect(Collectors.toMap(SchemaElement::getName, a -> a.getBizName(), (k1, k2) -> k1));
    }

    private String getQueryFilter(QueryFilters queryFilters) {
        if (Objects.isNull(queryFilters) || CollectionUtils.isEmpty(queryFilters.getFilters())) {
            return "";
        }
        List<QueryFilter> filters = queryFilters.getFilters();

        return filters.stream()
                .map(filter -> {
                    String bizNameWrap = StringUtil.getSpaceWrap(filter.getBizName());
                    String operatorWrap = StringUtil.getSpaceWrap(filter.getOperator().getValue());
                    String valueWrap = StringUtil.getCommaWrap(filter.getValue().toString());
                    return bizNameWrap + operatorWrap + valueWrap;
                })
                .collect(Collectors.joining(Constants.AND_UPPER));
    }
}
