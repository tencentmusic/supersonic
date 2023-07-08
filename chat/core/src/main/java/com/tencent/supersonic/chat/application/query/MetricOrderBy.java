package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import org.springframework.stereotype.Component;

@Component
public class MetricOrderBy extends RuleSemanticQuery {

    public static String QUERY_MODE = "METRIC_ORDERBY";

    public MetricOrderBy() {
        super();
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
        ContextHelper.addIfEmpty(chatParseInfo.getDimensions(), parseInfo.getDimensions());
        ContextHelper.addIfEmpty(chatParseInfo.getDimensionFilters(), parseInfo.getDimensionFilters());
    }

}
