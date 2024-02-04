package com.tencent.supersonic.chat.core.query.rule.metric;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.METRIC;
import static com.tencent.supersonic.chat.core.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.chat.core.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.ViewSchema;
import com.tencent.supersonic.chat.core.pojo.ChatContext;
import com.tencent.supersonic.chat.core.pojo.QueryContext;
import com.tencent.supersonic.chat.core.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.enums.TimeMode;
import com.tencent.supersonic.headless.api.pojo.TimeDefaultConfig;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class MetricSemanticQuery extends RuleSemanticQuery {

    private static final Long METRIC_MAX_RESULTS = 365L;

    public MetricSemanticQuery() {
        super();
        queryMatcher.addOption(METRIC, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public List<SchemaElementMatch> match(List<SchemaElementMatch> candidateElementMatches,
            QueryContext queryCtx) {
        return super.match(candidateElementMatches, queryCtx);
    }

    @Override
    public void fillParseInfo(QueryContext queryContext, ChatContext chatContext) {
        super.fillParseInfo(queryContext, chatContext);
        parseInfo.setLimit(METRIC_MAX_RESULTS);
        if (parseInfo.getDateInfo() == null) {
            ViewSchema viewSchema = queryContext.getSemanticSchema().getViewSchemaMap().get(parseInfo.getViewId());
            TimeDefaultConfig timeDefaultConfig = viewSchema.getMetricTypeTimeDefaultConfig();
            DateConf dateInfo = new DateConf();
            if (Objects.nonNull(timeDefaultConfig) && Objects.nonNull(timeDefaultConfig.getUnit())) {
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
            }
            parseInfo.setDateInfo(dateInfo);
        }
    }
}
