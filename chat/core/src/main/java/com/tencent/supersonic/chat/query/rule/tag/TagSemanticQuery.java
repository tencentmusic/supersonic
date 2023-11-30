package com.tencent.supersonic.chat.query.rule.tag;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatDefaultRichConfigResp;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.pojo.QueryType;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.ENTITY;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;

@Slf4j
public abstract class TagSemanticQuery extends RuleSemanticQuery {

    private static final Long TAG_MAX_RESULTS = 500L;

    public TagSemanticQuery() {
        super();
        queryMatcher.addOption(ENTITY, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public List<SchemaElementMatch> match(List<SchemaElementMatch> candidateElementMatches,
                                          QueryContext queryCtx) {
        return super.match(candidateElementMatches, queryCtx);
    }

    @Override
    public void fillParseInfo(ChatContext chatContext) {
        super.fillParseInfo(chatContext);

        parseInfo.setQueryType(QueryType.TAG);
        parseInfo.setLimit(TAG_MAX_RESULTS);
        if (parseInfo.getDateInfo() == null) {
            ConfigService configService = ContextUtils.getBean(ConfigService.class);
            ChatConfigRichResp chatConfig = configService.getConfigRichInfo(parseInfo.getModelId());
            ChatDefaultRichConfigResp defaultConfig = chatConfig.getChatDetailRichConfig().getChatDefaultConfig();

            int unit = 1;
            if (Objects.nonNull(defaultConfig) && Objects.nonNull(defaultConfig.getUnit())) {
                unit = defaultConfig.getUnit();
            }
            String date = LocalDate.now().plusDays(-unit).toString();
            DateConf dateInfo = new DateConf();
            dateInfo.setDateMode(DateConf.DateMode.BETWEEN);
            dateInfo.setStartDate(date);
            dateInfo.setEndDate(date);

            parseInfo.setDateInfo(dateInfo);
        }
    }

}
