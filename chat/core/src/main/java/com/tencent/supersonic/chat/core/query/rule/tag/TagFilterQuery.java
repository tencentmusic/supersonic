package com.tencent.supersonic.chat.core.query.rule.tag;

import static com.tencent.supersonic.chat.core.query.rule.QueryMatchOption.OptionType.OPTIONAL;
import static com.tencent.supersonic.chat.core.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.chat.core.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.TAG;
import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.TAG_VALUE;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class TagFilterQuery extends TagListQuery {

    public static final String QUERY_MODE = "TAG_LIST_FILTER";

    public TagFilterQuery() {
        super();
        queryMatcher.addOption(TAG, OPTIONAL, AT_LEAST, 0);
        queryMatcher.addOption(TAG_VALUE, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

}
