package com.tencent.supersonic.chat.query.rule.tag;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.VALUE;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class TagFilterQuery extends TagListQuery {

    public static final String QUERY_MODE = "TAG_LIST_FILTER";

    public TagFilterQuery() {
        super();
        queryMatcher.addOption(VALUE, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

}
