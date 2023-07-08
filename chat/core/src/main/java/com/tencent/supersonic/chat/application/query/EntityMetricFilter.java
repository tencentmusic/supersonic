package com.tencent.supersonic.chat.application.query;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.METRIC;
import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.VALUE;
import static com.tencent.supersonic.chat.application.query.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption.REQUIRED;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import org.springframework.stereotype.Component;

@Component
public class EntityMetricFilter extends EntitySemanticQuery {

    public static String QUERY_MODE = "ENTITY_METRIC_FILTER";

    public EntityMetricFilter() {
        super();
        queryMatcher.addOption(METRIC, REQUIRED, AT_LEAST, 1)
                .addOption(VALUE, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public void inheritContext(ChatContext chatContext) {
        SemanticParseInfo chatParseInfo = chatContext.getParseInfo();
        ContextHelper.addIfEmpty(chatParseInfo.getDimensionFilters(), parseInfo.getDimensionFilters());
    }

}
