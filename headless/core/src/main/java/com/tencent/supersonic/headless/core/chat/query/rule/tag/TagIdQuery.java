package com.tencent.supersonic.headless.core.chat.query.rule.tag;

import org.springframework.stereotype.Component;

import static com.tencent.supersonic.headless.core.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.headless.core.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.ID;

@Component
public class TagIdQuery extends TagListQuery {

    public static final String QUERY_MODE = "TAG_ID";

    public TagIdQuery() {
        super();
        queryMatcher.addOption(ID, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

}
