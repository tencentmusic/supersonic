package com.tencent.supersonic.chat.query.rule.entity;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.*;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.*;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class EntityFilterQuery extends EntityListQuery {

    public static final String QUERY_MODE = "ENTITY_LIST_FILTER";

    public EntityFilterQuery() {
        super();
        queryMatcher.addOption(VALUE, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

}
