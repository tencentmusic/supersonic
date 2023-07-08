package com.tencent.supersonic.chat.application.query;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.ENTITY;
import static com.tencent.supersonic.chat.application.query.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.chat.domain.pojo.chat.SchemaElementOption.REQUIRED;

public abstract class EntitySemanticQuery extends RuleSemanticQuery {

    public EntitySemanticQuery() {
        super();
        queryMatcher.addOption(ENTITY, REQUIRED, AT_LEAST, 1);
    }
}
