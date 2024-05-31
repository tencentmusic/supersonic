package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryReq;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.server.service.ChatQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class NL2SQLParser implements ChatParser {

    @Override
    public void parse(ChatParseContext chatParseContext, ParseResp parseResp) {
        if (!chatParseContext.enableNL2SQL()) {
            return;
        }
        if (checkSkip(parseResp)) {
            return;
        }
        QueryReq queryReq = QueryReqConverter.buildText2SqlQueryReq(chatParseContext);

        ChatQueryService chatQueryService = ContextUtils.getBean(ChatQueryService.class);
        ParseResp text2SqlParseResp = chatQueryService.performParsing(queryReq);
        if (!ParseResp.ParseState.FAILED.equals(text2SqlParseResp.getState())) {
            parseResp.getSelectedParses().addAll(text2SqlParseResp.getSelectedParses());
        }
        parseResp.getParseTimeCost().setSqlTime(text2SqlParseResp.getParseTimeCost().getSqlTime());
        formatParseResult(parseResp);
    }

    private boolean checkSkip(ParseResp parseResp) {
        List<SemanticParseInfo> selectedParses = parseResp.getSelectedParses();
        for (SemanticParseInfo semanticParseInfo : selectedParses) {
            if (semanticParseInfo.getScore() >= parseResp.getQueryText().length()) {
                return true;
            }
        }
        return false;
    }

    private void formatParseResult(ParseResp parseResp) {
        List<SemanticParseInfo> selectedParses = parseResp.getSelectedParses();
        for (SemanticParseInfo parseInfo : selectedParses) {
            formatParseInfo(parseInfo);
        }
    }

    private void formatParseInfo(SemanticParseInfo parseInfo) {
        if (!PluginQueryManager.isPluginQuery(parseInfo.getQueryMode())) {
            formatNL2SQLParseInfo(parseInfo);
        }
    }

    private void formatNL2SQLParseInfo(SemanticParseInfo parseInfo) {
        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append("**数据集:** ").append(parseInfo.getDataSet().getName()).append(" ");
        Optional<SchemaElement> metric = parseInfo.getMetrics().stream().findFirst();
        metric.ifPresent(schemaElement ->
                textBuilder.append("**指标:** ").append(schemaElement.getName()).append(" "));
        List<String> dimensionNames = parseInfo.getDimensions().stream()
                .map(SchemaElement::getName).filter(Objects::nonNull).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(dimensionNames)) {
            textBuilder.append("**维度:** ").append(String.join(",", dimensionNames));
        }
        textBuilder.append("\n\n**筛选条件:** \n");
        if (parseInfo.getDateInfo() != null) {
            textBuilder.append("**数据时间:** ").append(parseInfo.getDateInfo().getStartDate()).append("~")
                    .append(parseInfo.getDateInfo().getEndDate()).append(" ");
        }
        if (!CollectionUtils.isEmpty(parseInfo.getDimensionFilters())
                || CollectionUtils.isEmpty(parseInfo.getMetricFilters())) {
            Set<QueryFilter> queryFilters = parseInfo.getDimensionFilters();
            queryFilters.addAll(parseInfo.getMetricFilters());
            for (QueryFilter queryFilter : queryFilters) {
                textBuilder.append("**").append(queryFilter.getName()).append("**")
                        .append(" ")
                        .append(queryFilter.getOperator().getValue())
                        .append(" ")
                        .append(queryFilter.getValue())
                        .append(" ");
            }
        }
        parseInfo.setTextInfo(textBuilder.toString());
    }

}
