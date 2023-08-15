package com.tencent.supersonic.chat.query.rule.entity;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.ID;
import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.VALUE;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.OPTIONAL;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class EntityFilterQuery extends EntityListQuery {

    public static final String QUERY_MODE = "ENTITY_LIST_FILTER";

    public EntityFilterQuery() {
        super();
        queryMatcher.addOption(VALUE, OPTIONAL, AT_LEAST, 0);
        queryMatcher.addOption(ID, OPTIONAL, AT_LEAST, 0);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

}
