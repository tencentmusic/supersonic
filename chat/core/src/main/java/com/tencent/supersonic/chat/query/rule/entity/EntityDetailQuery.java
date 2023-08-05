package com.tencent.supersonic.chat.query.rule.entity;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.*;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;

import org.springframework.stereotype.Component;

@Component
public class EntityDetailQuery extends EntitySemanticQuery {

    public static final String QUERY_MODE = "ENTITY_DETAIL";

    public EntityDetailQuery() {
        super();
        queryMatcher.addOption(DIMENSION, REQUIRED, AT_LEAST, 1)
                .addOption(ID, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

}
