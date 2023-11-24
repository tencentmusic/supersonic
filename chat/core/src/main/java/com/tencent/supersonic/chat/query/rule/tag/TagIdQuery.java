package com.tencent.supersonic.chat.query.rule.tag;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.ID;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

import org.springframework.stereotype.Component;

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
