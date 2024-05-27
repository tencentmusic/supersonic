package com.tencent.supersonic.headless.core.chat.corrector;

import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticSchema;
import com.tencent.supersonic.headless.api.pojo.SqlInfo;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Perform SQL corrections on the "Group by" section in S2SQL.
 */
@Slf4j
public class GroupByCorrector extends BaseSemanticCorrector {

    @Override
    public void doCorrect(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        Boolean needAddGroupBy = needAddGroupBy(queryContext, semanticParseInfo);
        if (!needAddGroupBy) {
            return;
        }
        addGroupByFields(queryContext, semanticParseInfo);
    }

    private Boolean needAddGroupBy(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        Long dataSetId = semanticParseInfo.getDataSetId();
        //add dimension group by
        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();
        String correctS2SQL = sqlInfo.getCorrectS2SQL();
        SemanticSchema semanticSchema = queryContext.getSemanticSchema();
        // check has distinct
        if (SqlSelectHelper.hasDistinct(correctS2SQL)) {
            log.info("not add group by ,exist distinct in correctS2SQL:{}", correctS2SQL);
            return false;
        }
        //add alias field name
        Set<String> dimensions = getDimensions(dataSetId, semanticSchema);
        List<String> selectFields = SqlSelectHelper.getSelectFields(correctS2SQL);
        if (CollectionUtils.isEmpty(selectFields) || CollectionUtils.isEmpty(dimensions)) {
            return false;
        }
        // if only date in select not add group by.
        if (selectFields.size() == 1 && selectFields.contains(TimeDimensionEnum.DAY.getChName())) {
            return false;
        }
        if (SqlSelectHelper.hasGroupBy(correctS2SQL)) {
            log.info("not add group by ,exist group by in correctS2SQL:{}", correctS2SQL);
            return false;
        }
        Environment environment = ContextUtils.getBean(Environment.class);
        String correctorAdditionalInfo = environment.getProperty("s2.corrector.additional.information");
        if (StringUtils.isNotBlank(correctorAdditionalInfo) && !Boolean.parseBoolean(correctorAdditionalInfo)) {
            return false;
        }
        return true;
    }

    private void addGroupByFields(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        Long dataSetId = semanticParseInfo.getDataSetId();
        //add dimension group by
        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();
        String correctS2SQL = sqlInfo.getCorrectS2SQL();
        SemanticSchema semanticSchema = queryContext.getSemanticSchema();
        //add alias field name
        Set<String> dimensions = getDimensions(dataSetId, semanticSchema);
        List<String> selectFields = SqlSelectHelper.getSelectFields(correctS2SQL);
        List<String> aggregateFields = SqlSelectHelper.getAggregateFields(correctS2SQL);
        Set<String> groupByFields = selectFields.stream()
                .filter(field -> dimensions.contains(field))
                .filter(field -> {
                    if (!CollectionUtils.isEmpty(aggregateFields) && aggregateFields.contains(field)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toSet());
        semanticParseInfo.getSqlInfo().setCorrectS2SQL(SqlAddHelper.addGroupBy(correctS2SQL, groupByFields));
        addAggregate(queryContext, semanticParseInfo);
    }

    private void addAggregate(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        List<String> sqlGroupByFields = SqlSelectHelper.getGroupByFields(
                semanticParseInfo.getSqlInfo().getCorrectS2SQL());
        if (CollectionUtils.isEmpty(sqlGroupByFields)) {
            return;
        }
        addAggregateToMetric(queryContext, semanticParseInfo);
    }
}
