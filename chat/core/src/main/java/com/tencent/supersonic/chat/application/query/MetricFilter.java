package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import org.springframework.stereotype.Component;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.*;
import static com.tencent.supersonic.chat.application.query.QueryMatchOption.RequireNumberType.*;
import static com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption.*;

@Component
public class MetricFilter extends MetricSemanticQuery {

    public static String QUERY_MODE = "METRIC_FILTER";

    public MetricFilter() {
        super();
        queryMatcher.addOption(VALUE, REQUIRED, AT_LEAST, 1)
                .addOption(ENTITY, OPTIONAL, AT_MOST, 1);

    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public void inheritContext(ChatContext chatContext) {
        SemanticParseInfo chatParseInfo = chatContext.getParseInfo();
        ContextHelper.updateTimeIfEmpty(chatParseInfo, parseInfo);
        ContextHelper.addIfEmpty(chatParseInfo.getMetrics(), parseInfo.getMetrics());
        ContextHelper.addIfEmpty(chatParseInfo.getDimensionFilters(), parseInfo.getDimensionFilters());
    }
}
