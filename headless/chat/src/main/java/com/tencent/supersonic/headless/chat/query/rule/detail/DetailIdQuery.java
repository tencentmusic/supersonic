package com.tencent.supersonic.headless.chat.query.rule.detail;

import org.springframework.stereotype.Component;

import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.ID;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

@Component
public class DetailIdQuery extends DetailListQuery {

    public static final String QUERY_MODE = "DETAIL_ID";

    public DetailIdQuery() {
        super();
        queryMatcher.addOption(ID, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

}
