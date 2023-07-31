package com.tencent.supersonic.chat.query.rule.metric;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.QueryContext;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.config.ChatConfigResp;
import com.tencent.supersonic.chat.config.ChatConfigRich;
import com.tencent.supersonic.chat.config.ChatDefaultRichConfig;
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

import static com.tencent.supersonic.chat.api.pojo.SchemaElementType.METRIC;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.RequireNumberType.AT_LEAST;
import static com.tencent.supersonic.chat.query.rule.QueryMatchOption.OptionType.REQUIRED;

@Slf4j
public abstract class MetricSemanticQuery extends RuleSemanticQuery {

    private static final Long METRIC_MAX_RESULTS = 365L;

    public MetricSemanticQuery() {
        super();
        queryMatcher.addOption(METRIC, REQUIRED, AT_LEAST, 1);
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
                || Objects.isNull(candidateElementMatches.get(0).getElement().getDomain())) {
            return candidateElementMatches;
        }

        Long domainId = candidateElementMatches.get(0).getElement().getDomain();
        ConfigService configService = ContextUtils.getBean(ConfigService.class);
        ChatConfigResp chatConfig = configService.fetchConfigByDomainId(domainId);

        List<Long> blackDimIdList = new ArrayList<>();
        List<Long> blackMetricIdList = new ArrayList<>();
        if (Objects.nonNull(chatConfig.getChatAggConfig())
                && Objects.nonNull(chatConfig.getChatAggConfig().getVisibility())) {
            blackDimIdList = chatConfig.getChatAggConfig().getVisibility().getBlackDimIdList();
            blackMetricIdList = chatConfig.getChatAggConfig().getVisibility().getBlackMetricIdList();
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
    public void fillParseInfo(Long domainId, ChatContext chatContext){
        super.fillParseInfo(domainId, chatContext);

        parseInfo.setLimit(METRIC_MAX_RESULTS);
        if (parseInfo.getDateInfo() == null) {
            ConfigService configService = ContextUtils.getBean(ConfigService.class);
            ChatConfigRich chatConfig = configService.getConfigRichInfo(parseInfo.getDomainId());
            ChatDefaultRichConfig defaultConfig = chatConfig.getChatAggRichConfig().getChatDefaultConfig();

            int unit = 1;
            if (Objects.nonNull(defaultConfig) && Objects.nonNull(defaultConfig.getUnit())) {
                unit = defaultConfig.getUnit();
            }
            String startDate = LocalDate.now().plusDays(-unit).toString();
            String endDate = LocalDate.now().plusDays(-1).toString();
            DateConf dateInfo = new DateConf();
            dateInfo.setDateMode(DateConf.DateMode.BETWEEN_CONTINUOUS);
            dateInfo.setStartDate(startDate);
            dateInfo.setEndDate(endDate);

            parseInfo.setDateInfo(dateInfo);
        }
    }

}
