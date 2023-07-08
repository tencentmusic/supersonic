package com.tencent.supersonic.chat.application.query;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.*;
import static com.tencent.supersonic.chat.application.query.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption.REQUIRED;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.domain.utils.ContextHelper;
import org.springframework.stereotype.Component;

@Component
public class EntityDetail extends EntitySemanticQuery {

    public static String QUERY_MODE = "ENTITY_DETAIL";

    public EntityDetail() {
        super();
        queryMatcher.addOption(DIMENSION, REQUIRED, AT_LEAST, 1)
                .addOption(VALUE, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public void inheritContext(ChatContext chatContext) {
        ContextHelper.addIfEmpty(chatContext.getParseInfo().getDimensionFilters(),
                parseInfo.getDimensionFilters());
    }

}
