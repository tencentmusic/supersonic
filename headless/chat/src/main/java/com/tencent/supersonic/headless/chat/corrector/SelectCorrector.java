package com.tencent.supersonic.headless.chat.corrector;

import com.tencent.supersonic.common.jsqlparser.FieldExpression;
import com.tencent.supersonic.common.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.common.jsqlparser.SqlValidHelper;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;

/** Perform SQL corrections on the "Select" section in S2SQL. */
@Slf4j
public class SelectCorrector extends BaseSemanticCorrector {

    public static final String ADDITIONAL_INFORMATION = "s2.corrector.additional.information";

    @Override
    public void doCorrect(ChatQueryContext chatQueryContext, SemanticParseInfo semanticParseInfo) {
        String correctS2SQL = semanticParseInfo.getSqlInfo().getCorrectedS2SQL();
        if (SqlValidHelper.isComplexSQL(correctS2SQL)) {
            return;
        }
        List<String> aggregateFields = SqlSelectHelper.getAggregateFields(correctS2SQL);
        List<String> selectFields = SqlSelectHelper.getSelectFields(correctS2SQL);
        // If the number of aggregated fields is equal to the number of queried fields, do not add
        // fields to select.
        if (!CollectionUtils.isEmpty(aggregateFields) && !CollectionUtils.isEmpty(selectFields)
                && aggregateFields.size() == selectFields.size()) {
            return;
        }
        correctS2SQL = addFieldsToSelect(semanticParseInfo, correctS2SQL);
        semanticParseInfo.getSqlInfo().setCorrectedS2SQL(correctS2SQL);
    }

    protected String addFieldsToSelect(SemanticParseInfo semanticParseInfo, String correctS2SQL) {

        Set<String> selectFields = new HashSet<>(SqlSelectHelper.getSelectFields(correctS2SQL));
        Set<String> needAddFields = new HashSet<>(SqlSelectHelper.getGroupByFields(correctS2SQL));

        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(needAddFields)) {
            return correctS2SQL;
        }
        needAddFields.removeAll(selectFields);

        if (!SqlSelectHelper.hasSubSelect(correctS2SQL)) { // 优化内容 ， 如果sql 条件包含了这个字段，而且是全等，则不再查询该字段
            List<FieldExpression> tmp4 = SqlSelectHelper.getWhereExpressions(correctS2SQL);
            Iterator<String> it = needAddFields.iterator();
            while (it.hasNext()) {
                String field = it.next();
                long size = tmp4.stream()
                        .filter(e -> e.getFieldName().equals(field) && "=".equals(e.getOperator()))
                        .count();
                if (size == 1) {
                    it.remove();
                }
            }
        }
        if (needAddFields.size() > 0) {
            String addFieldsToSelectSql =
                    SqlAddHelper.addFieldsToSelect(correctS2SQL, new ArrayList<>(needAddFields));
            semanticParseInfo.getSqlInfo().setCorrectedS2SQL(addFieldsToSelectSql);
            return addFieldsToSelectSql;
        } else {
            return correctS2SQL;
        }
    }

}
