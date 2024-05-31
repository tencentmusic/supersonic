package com.tencent.supersonic.headless.core.chat.query.rule.detail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.VALUE;
import static com.tencent.supersonic.headless.core.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.headless.core.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

@Slf4j
@Component
public class DetailFilterQuery extends DetailListQuery {

    public static final String QUERY_MODE = "DETAIL_LIST_FILTER";

    public DetailFilterQuery() {
        super();
        queryMatcher.addOption(VALUE, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

}
