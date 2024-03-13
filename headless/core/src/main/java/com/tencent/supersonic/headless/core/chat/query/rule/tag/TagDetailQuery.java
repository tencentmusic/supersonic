package com.tencent.supersonic.headless.core.chat.query.rule.tag;

import org.springframework.stereotype.Component;

import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.TAG;
import static com.tencent.supersonic.headless.core.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.headless.core.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.ID;

@Component
public class TagDetailQuery extends TagSemanticQuery {

    public static final String QUERY_MODE = "TAG_DETAIL";

    public TagDetailQuery() {
        super();
        queryMatcher.addOption(TAG, REQUIRED, AT_LEAST, 1)
                .addOption(ID, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

}
