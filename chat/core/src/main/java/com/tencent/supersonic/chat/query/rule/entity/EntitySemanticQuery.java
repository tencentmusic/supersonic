package com.tencent.supersonic.chat.query.rule.entity;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatDefaultRichConfigResp;
import com.tencent.supersonic.chat.query.rule.RuleSemanticQuery;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.common.pojo.DateConf;
import com.tencent.supersonic.common.util.ContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.ENTITY;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;

@Slf4j
public abstract class EntitySemanticQuery extends RuleSemanticQuery {

    private static final Long ENTITY_MAX_RESULTS = 500L;

    public EntitySemanticQuery() {
        super();
        queryMatcher.addOption(ENTITY, REQUIRED, AT_LEAST, 1);
    }

    @Override
    public List<SchemaElementMatch> match(List<SchemaElementMatch> candidateElementMatches,
                                          QueryContext queryCtx) {
        candidateElementMatches = filterElementMatches(candidateElementMatches);
        return super.match(candidateElementMatches, queryCtx);
    }

    private List<SchemaElementMatch> filterElementMatches(List<SchemaElementMatch> candidateElementMatches) {
        List<SchemaElementMatch> filteredMatches = new ArrayList<>();
        if (CollectionUtils.isEmpty(candidateElementMatches)
                || Objects.isNull(candidateElementMatches.get(0).getElement().getModel())) {
            return candidateElementMatches;
        }

        Long modelId = candidateElementMatches.get(0).getElement().getModel();
        ConfigService configService = ContextUtils.getBean(ConfigService.class);
        ChatConfigResp chatConfig = configService.fetchConfigByModelId(modelId);

        List<Long> blackDimIdList = new ArrayList<>();
        List<Long> blackMetricIdList = new ArrayList<>();
        if (Objects.nonNull(chatConfig.getChatDetailConfig())
                && Objects.nonNull(chatConfig.getChatDetailConfig().getVisibility())) {
            blackDimIdList = chatConfig.getChatDetailConfig().getVisibility().getBlackDimIdList();
            blackMetricIdList = chatConfig.getChatDetailConfig().getVisibility().getBlackMetricIdList();
        }

        for (SchemaElementMatch schemaElementMatch : candidateElementMatches) {

            SchemaElementType type = schemaElementMatch.getElement().getType();
            if (SchemaElementType.DIMENSION.equals(type) || SchemaElementType.VALUE.equals(type)) {
                if (!blackDimIdList.contains(schemaElementMatch.getElement().getId())) {
                    filteredMatches.add(schemaElementMatch);
                }
            } else if (SchemaElementType.METRIC.equals(type)) {
                if (!blackMetricIdList.contains(schemaElementMatch.getElement().getId())) {
                    filteredMatches.add(schemaElementMatch);
                }
            } else {
                filteredMatches.add(schemaElementMatch);
            }
        }
        return filteredMatches;
    }

    @Override
    public void fillParseInfo(Long modelId, QueryContext queryContext, ChatContext chatContext) {
        super.fillParseInfo(modelId, queryContext, chatContext);

        parseInfo.setNativeQuery(true);
        parseInfo.setLimit(ENTITY_MAX_RESULTS);
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
