package com.tencent.supersonic.chat.application.query;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import org.springframework.stereotype.Component;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.*;
import static com.tencent.supersonic.chat.application.query.QueryMatchOption.RequireNumberType.*;
import static com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption.*;

@Component
public class MetricGroupBy extends MetricSemanticQuery {

    public static String QUERY_MODE = "METRIC_GROUPBY";

    public MetricGroupBy() {
        super();
        queryMatcher.addOption(DIMENSION, REQUIRED, AT_LEAST, 1);
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
    }

}
