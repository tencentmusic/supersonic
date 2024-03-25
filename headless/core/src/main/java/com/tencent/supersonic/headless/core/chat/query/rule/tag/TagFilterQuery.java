package com.tencent.supersonic.headless.core.chat.query.rule.tag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.TAG;
import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.TAG_VALUE;
import static com.tencent.supersonic.headless.core.chat.query.rule.QueryMatchOption.OptionType.OPTIONAL;
import static com.tencent.supersonic.headless.core.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.headless.core.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

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
