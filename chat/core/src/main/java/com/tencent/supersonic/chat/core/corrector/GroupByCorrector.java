package com.tencent.supersonic.chat.core.corrector;

import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticSchema;
import com.tencent.supersonic.chat.api.pojo.response.SqlInfo;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.common.pojo.enums.TimeDimensionEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.jsqlparser.SqlAddHelper;
import com.tencent.supersonic.common.util.jsqlparser.SqlSelectHelper;
import com.tencent.supersonic.headless.api.pojo.Dim;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.ViewResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.ModelService;
import com.tencent.supersonic.headless.server.service.ViewService;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

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
        Long viewId = semanticParseInfo.getViewId();
        ViewService viewService = ContextUtils.getBean(ViewService.class);
        ModelService modelService = ContextUtils.getBean(ModelService.class);
        ViewResp viewResp = viewService.getView(viewId);
        List<Long> modelIds = viewResp.getViewDetail().getViewModelConfigs().stream().map(config -> config.getId())
                .collect(Collectors.toList());
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setIds(modelIds);
        List<ModelResp> modelRespList = modelService.getModelList(metaFilter);
        for (ModelResp modelResp : modelRespList) {
            List<Dim> dimList = modelResp.getModelDetail().getDimensions();
            for (Dim dim : dimList) {
                if (Objects.nonNull(dim.getTypeParams()) && dim.getTypeParams().getTimeGranularity().equals("none")) {
                    return false;
                }
            }
        }
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
        Set<String> dimensions = getDimensions(viewId, semanticSchema);
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
        return true;
    }

    private void addGroupByFields(QueryContext queryContext, SemanticParseInfo semanticParseInfo) {
        Long viewId = semanticParseInfo.getViewId();
        //add dimension group by
        SqlInfo sqlInfo = semanticParseInfo.getSqlInfo();
        String correctS2SQL = sqlInfo.getCorrectS2SQL();
        SemanticSchema semanticSchema = queryContext.getSemanticSchema();
        //add alias field name
        Set<String> dimensions = getDimensions(viewId, semanticSchema);
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
