package com.tencent.supersonic.headless.chat.query.rule.detail;

import com.tencent.supersonic.headless.api.pojo.DataSetSchema;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import org.springframework.stereotype.Component;

import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.DIMENSION;
import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.METRIC;
import static com.tencent.supersonic.headless.api.pojo.SchemaElementType.VALUE;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.OptionType.OPTIONAL;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.headless.chat.query.rule.QueryMatchOption.RequireNumberType.AT_MOST;

@Component
public class DetailValueQuery extends DetailSemanticQuery {

    public static final String QUERY_MODE = "DETAIL_VALUE";

    public DetailValueQuery() {
        super();
        queryMatcher.addOption(VALUE, REQUIRED, AT_LEAST, 1);
        queryMatcher.addOption(DIMENSION, OPTIONAL, AT_MOST, 0);
        queryMatcher.addOption(METRIC, OPTIONAL, AT_MOST, 0);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public void fillParseInfo(ChatQueryContext chatQueryContext, Long dataSetId) {
        super.fillParseInfo(chatQueryContext, dataSetId);

        DataSetSchema dataSetSchema = chatQueryContext.getDataSetSchema(dataSetId);
        parseInfo.getDimensions().addAll(dataSetSchema.getDimensions());
        parseInfo.getDimensions().forEach(
                d -> parseInfo.getElementMatches().add(SchemaElementMatch.builder().element(d)
                        .word(d.getName()).similarity(0).detectWord(d.getName()).build()));

    }

}
