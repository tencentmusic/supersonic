package com.tencent.supersonic.chat.application.query;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.*;
import static com.tencent.supersonic.chat.application.query.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption.REQUIRED;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import org.springframework.stereotype.Component;

@Component
public class EntityListTopN extends EntitySemanticQuery {

    public static String QUERY_MODE = "ENTITY_LIST_TOPN";

    public EntityListTopN() {
        super();
        queryMatcher.addOption(METRIC, REQUIRED, AT_LEAST, 1)
                .setSupportOrderBy(true);
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
    }

}
