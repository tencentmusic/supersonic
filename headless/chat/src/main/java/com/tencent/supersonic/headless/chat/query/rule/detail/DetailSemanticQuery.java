package com.tencent.supersonic.headless.chat.query.rule.detail;

import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.QueryType;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.query.rule.RuleSemanticQuery;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public abstract class DetailSemanticQuery extends RuleSemanticQuery {

    private static final Long DETAIL_MAX_RESULTS = 500L;

    public DetailSemanticQuery() {
        super();
    }

    @Override
    public List<SchemaElementMatch> match(
            List<SchemaElementMatch> candidateElementMatches, ChatQueryContext queryCtx) {
        return super.match(candidateElementMatches, queryCtx);
    }

    @Override
    public void fillParseInfo(ChatQueryContext chatQueryContext) {
        super.fillParseInfo(chatQueryContext);

        parseInfo.setQueryType(QueryType.DETAIL);
        parseInfo.setLimit(DETAIL_MAX_RESULTS);
        if (!needFillDateConf(chatQueryContext)) {
            return;
        }
        Map<Long, DataSetSchema> dataSetSchemaMap =
                chatQueryContext.getSemanticSchema().getDataSetSchemaMap();
        DataSetSchema dataSetSchema = dataSetSchemaMap.get(parseInfo.getDataSetId());
        TimeDefaultConfig timeDefaultConfig = dataSetSchema.getTagTypeTimeDefaultConfig();

        if (Objects.nonNull(timeDefaultConfig)
                && Objects.nonNull(timeDefaultConfig.getUnit())
                && timeDefaultConfig.getUnit() != -1) {
            DateConf dateInfo = new DateConf();
            int unit = timeDefaultConfig.getUnit();
            String startDate = LocalDate.now().plusDays(-unit).toString();
            String endDate = startDate;
            if (TimeMode.LAST.equals(timeDefaultConfig.getTimeMode())) {
                dateInfo.setDateMode(DateConf.DateMode.BETWEEN);
            } else if (TimeMode.RECENT.equals(timeDefaultConfig.getTimeMode())) {
                dateInfo.setDateMode(DateConf.DateMode.RECENT);
                endDate = LocalDate.now().plusDays(-1).toString();
            }
            dateInfo.setUnit(unit);
            dateInfo.setPeriod(timeDefaultConfig.getPeriod());
            dateInfo.setStartDate(startDate);
            dateInfo.setEndDate(endDate);
            parseInfo.setDateInfo(dateInfo);
        }
    }
}
