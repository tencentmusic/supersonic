package com.tencent.supersonic.chat.server.processor.execute;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.server.pojo.ExecuteContext;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.server.pojo.DimensionsFilter;
import com.tencent.supersonic.headless.server.pojo.MetricsFilter;
import com.tencent.supersonic.headless.server.service.DataSetService;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.MetricService;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author roylin
 * @since 2024/10/30 16:01
 */
@Slf4j
public class DictionaryRewriteProcessor implements ExecuteResultProcessor {
    @Override
    public void process(ExecuteContext executeContext, QueryResult queryResult) {
        String[] metaKeyWords = Constants.META_DICT_QUERY_KEYWORD.split(",");
        boolean isContainMetaKeyWord = Arrays.stream(metaKeyWords)
                .anyMatch(s -> executeContext.getRequest().getQueryText().contains(s));

        if (!isContainMetaKeyWord) {
            return;
        }


        DataSetService dataSetService = ContextUtils.getBean(DataSetService.class);
        Map<Long, List<Long>> modelIdMap = dataSetService.getModelIdToDataSetIds(
                Lists.newArrayList(executeContext.getAgent().getDataSetIds()),
                User.getDefaultUser());
        DimensionService dimensionService = ContextUtils.getBean(DimensionService.class);
        DimensionsFilter dimensionsFilter = new DimensionsFilter();
        dimensionsFilter.setModelIds(Lists.newArrayList(modelIdMap.keySet()));
        List<DimensionResp> dimensionResps = dimensionService.queryDimensions(dimensionsFilter);

        MetricService metricService = ContextUtils.getBean(MetricService.class);
        MetricsFilter metricsFilter = new MetricsFilter();
        metricsFilter.setModelIds(Lists.newArrayList(modelIdMap.keySet()));
        List<MetricResp> metricResps = metricService.queryMetrics(metricsFilter);


        List<QueryColumn> queryColumnList = Lists.newArrayList();

        QueryColumn queryColumn1 = new QueryColumn("名称", "VARCHAR", "name");
        QueryColumn queryColumn2 = new QueryColumn("字段类型", "VARCHAR", "dimType");

        QueryColumn queryColumn3 = new QueryColumn("字段描述", "VARCHAR", "bizName");

        queryColumnList.add(queryColumn1);
        queryColumnList.add(queryColumn2);
        queryColumnList.add(queryColumn3);

        queryResult.setQueryColumns(queryColumnList);

        List<Map<String, Object>> queryResultList = Lists.newArrayList();

        dimensionResps.forEach(dimensionResp -> {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("name", dimensionResp.getName());
            dataMap.put("dimType", "维度");
            dataMap.put("bizName", dimensionResp.getDescription());
            queryResultList.add(dataMap);
        });

        metricResps.forEach(metricResp -> {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("name", metricResp.getName());
            dataMap.put("dimType", "指标");
            dataMap.put("bizName", metricResp.getDescription());
            queryResultList.add(dataMap);
        });


        queryResult.setQueryResults(queryResultList);


    }
}
