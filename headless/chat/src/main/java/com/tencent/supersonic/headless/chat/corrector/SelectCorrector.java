package com.tencent.supersonic.headless.chat.corrector;


import com.tencent.supersonic.common.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.jsqlparser.SqlRemoveHelper;
import com.tencent.supersonic.common.jsqlparser.SqlReplaceHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectFunctionHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.QueryContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Perform SQL corrections on the "Select" section in S2SQL.
 */
@Slf4j
public class SelectCorrector extends BaseSemanticCorrector {

    public static final String ADDITIONAL_INFORMATION = "s2.corrector.additional.information";

    @Override
    public void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectS2SQL();
        List<String> aggregateFields = SqlSelectHelper.getAggregateFields(correctS2SQL);
        List<String> selectFields = SqlSelectHelper.getSelectFields(correctS2SQL);
        // If the number of aggregated fields is equal to the number of queried fields, do not add fields to select.
        if (!CollectionUtils.isEmpty(aggregateFields)
                && !CollectionUtils.isEmpty(selectFields)
                && aggregateFields.size() == selectFields.size()) {
            return;
        }
        correctS2SQL = addFieldsToSelect(queryContext, semanticParseInfo, correctS2SQL);
        String querySql = SqlReplaceHelper.dealAliasToOrderBy(correctS2SQL);
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(querySql);
    }

    protected String addFieldsToSelect(QueryContext queryContext, SemanticParseInfo semanticParseInfo,
                                       String correctS2SQL) {
        correctS2SQL = addTagDefaultFields(queryContext, semanticParseInfo, correctS2SQL);

        Set<String> selectFields = new HashSet<>(SqlSelectHelper.getSelectFields(correctS2SQL));
        Set<String> needAddFields = new HashSet<>(SqlSelectHelper.getGroupByFields(correctS2SQL));

        //decide whether add order by expression field to select
        Environment environment = ContextUtils.getBean(Environment.class);
        String correctorAdditionalInfo = environment.getProperty(ADDITIONAL_INFORMATION);
        if (StringUtils.isNotBlank(correctorAdditionalInfo) && Boolean.parseBoolean(correctorAdditionalInfo)) {
            needAddFields.addAll(SqlSelectHelper.getOrderByFields(correctS2SQL));
        }
        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(needAddFields)) {
            return correctS2SQL;
        }
        needAddFields.removeAll(selectFields);
        String addFieldsToSelectSql = SqlAddHelper.addFieldsToSelect(correctS2SQL, new ArrayList<>(needAddFields));
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(addFieldsToSelectSql);
        return addFieldsToSelectSql;
    }

    private String addTagDefaultFields(QueryContext queryContext, SemanticParseInfo semanticParseInfo,
                                       String correctS2SQL) {
        //If it is in DETAIL mode and select *, add default metrics and dimensions.
        boolean hasAsterisk = SqlSelectFunctionHelper.hasAsterisk(correctS2SQL);
        if (!(hasAsterisk && QueryType.DETAIL.equals(semanticParseInfo.getQueryType()))) {
            return correctS2SQL;
        }
        Long dataSetId = semanticParseInfo.getDataSetId();
        DataSetSchema dataSetSchema = queryContext.getSemanticSchema().getDataSetSchemaMap().get(dataSetId);
        Set<String> needAddDefaultFields = new HashSet<>();
        if (Objects.nonNull(dataSetSchema)) {
            if (!CollectionUtils.isEmpty(dataSetSchema.getTagDefaultMetrics())) {
                Set<String> metrics = dataSetSchema.getTagDefaultMetrics()
                        .stream().map(schemaElement -> schemaElement.getName())
                        .collect(Collectors.toSet());
                needAddDefaultFields.addAll(metrics);
            }
            if (!CollectionUtils.isEmpty(dataSetSchema.getTagDefaultDimensions())) {
                Set<String> dimensions = dataSetSchema.getTagDefaultDimensions()
                        .stream().map(schemaElement -> schemaElement.getName())
                        .collect(Collectors.toSet());
                needAddDefaultFields.addAll(dimensions);
            }
        }
        // remove * in sql and add default fields.
        if (!CollectionUtils.isEmpty(needAddDefaultFields)) {
            correctS2SQL = SqlRemoveHelper.removeAsteriskAndAddFields(correctS2SQL, needAddDefaultFields);
        }
        return correctS2SQL;
    }
}
