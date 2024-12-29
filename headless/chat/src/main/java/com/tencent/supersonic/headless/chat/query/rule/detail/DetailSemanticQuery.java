package com.tencent.supersonic.headless.chat.query.rule.detail;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.query.rule.RuleSemanticQuery;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

@Slf4j
public abstract class DetailSemanticQuery extends RuleSemanticQuery {

    public DetailSemanticQuery() {
        super();
    }

    @Override
    public void fillParseInfo(ChatQueryContext chatQueryContext, Long dataSetId) {
        super.fillParseInfo(chatQueryContext, dataSetId);

        parseInfo.setLimit(parseInfo.getDetailLimit());
        if (!needFillDateConf(chatQueryContext)) {
            return;
        }
        Map<Long, DataSetSchema> dataSetSchemaMap =
                chatQueryContext.getSemanticSchema().getDataSetSchemaMap();
        DataSetSchema dataSetSchema = dataSetSchemaMap.get(parseInfo.getDataSetId());
        TimeDefaultConfig timeDefaultConfig = dataSetSchema.getDetailTypeTimeDefaultConfig();
        SchemaElement partitionDimension = dataSetSchema.getPartitionDimension();

        if (Objects.nonNull(partitionDimension) && Objects.nonNull(timeDefaultConfig)
                && Objects.nonNull(timeDefaultConfig.getUnit())
                && timeDefaultConfig.getUnit() != -1) {
            DateConf dateInfo = new DateConf();
            dateInfo.setDateField(partitionDimension.getName());
            int unit = timeDefaultConfig.getUnit();
            String startDate = LocalDate.now().minusDays(unit).toString();
            String endDate = startDate;
            dateInfo.setDateMode(DateConf.DateMode.BETWEEN);
            if (TimeMode.RECENT.equals(timeDefaultConfig.getTimeMode())) {
                endDate = LocalDate.now().toString();
            }
            dateInfo.setUnit(unit);
            dateInfo.setPeriod(timeDefaultConfig.getPeriod());
            dateInfo.setStartDate(startDate);
            dateInfo.setEndDate(endDate);
            parseInfo.setDateInfo(dateInfo);
        }
    }
}
