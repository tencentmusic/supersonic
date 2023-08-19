package com.tencent.supersonic.chat.query.rule.entity;

import org.springframework.stereotype.Component;
import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.ID;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

@Component
public class EntityIdQuery extends EntityListQuery {

    public static final String QUERY_MODE = "ENTITY_ID";

    public EntityIdQuery() {
        super();
        queryMatcher.addOption(ID, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

}
