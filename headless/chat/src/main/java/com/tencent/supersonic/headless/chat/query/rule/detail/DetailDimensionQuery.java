package com.tencent.supersonic.headless.chat.query.rule.detail;

import org.springframework.stereotype.Component;

import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.DIMENSION;
import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.ID;
import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.VALUE;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.OptionType.OPTIONAL;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

@Component
public class DetailDimensionQuery extends DetailSemanticQuery {

    public static final String QUERY_MODE = "DETAIL_DIMENSION";

    public DetailDimensionQuery() {
        super();
        queryMatcher.addOption(DIMENSION, REQUIRED, AT_LEAST, 1);
        queryMatcher.addOption(VALUE, OPTIONAL, AT_LEAST, 0);
        queryMatcher.addOption(ID, OPTIONAL, AT_LEAST, 0);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }
}
