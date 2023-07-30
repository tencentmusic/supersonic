package com.tencent.supersonic.chat.query.rule.entity;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.*;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;

import org.springframework.stereotype.Component;

@Component
public class EntityTopNQuery extends EntityListQuery {

    public static final String QUERY_MODE = "ENTITY_LIST_TOPN";

    public EntityTopNQuery() {
        super();
        queryMatcher.addOption(METRIC, REQUIRED, AT_LEAST, 1)
                .setSupportOrderBy(true);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

}
