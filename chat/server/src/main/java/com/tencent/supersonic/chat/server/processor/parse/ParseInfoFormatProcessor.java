package com.tencent.supersonic.chat.server.processor.parse;

import com.tencent.supersonic.chat.server.plugin.PluginQueryManager;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ParseInfoFormatProcessor formats parse info to make it more readable to the users.
 **/
public class ParseInfoFormatProcessor implements ParseResultProcessor {
    @Override
    public void process(ParseContext parseContext) {
        parseContext.getResponse().getSelectedParses().forEach(p -> {
            if (!PluginQueryManager.isPluginQuery(p.getQueryMode())) {
                formatNL2SQLParseInfo(p);
            }
        });
    }

    private static void formatNL2SQLParseInfo(SemanticParseInfo parseInfo) {
        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append("**数据集:** ").append(parseInfo.getDataSet().getName()).append(" ");
        Optional<SchemaElement> metric = parseInfo.getMetrics().stream().findFirst();
        metric.ifPresent(schemaElement -> textBuilder.append("**指标:** ")
                .append(schemaElement.getName()).append(" "));
        List<String> dimensionNames = parseInfo.getDimensions().stream().map(SchemaElement::getName)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(dimensionNames)) {
            textBuilder.append("**维度:** ").append(String.join(",", dimensionNames));
        }
        textBuilder.append("\n\n**筛选条件:** \n");
        if (parseInfo.getDateInfo() != null) {
            textBuilder.append("**数据时间:** ").append(parseInfo.getDateInfo().getStartDate())
                    .append("~").append(parseInfo.getDateInfo().getEndDate()).append(" ");
        }
        if (!CollectionUtils.isEmpty(parseInfo.getDimensionFilters())
                || CollectionUtils.isEmpty(parseInfo.getMetricFilters())) {
            Set<QueryFilter> queryFilters = parseInfo.getDimensionFilters();
            queryFilters.addAll(parseInfo.getMetricFilters());
            for (QueryFilter queryFilter : queryFilters) {
                textBuilder.append("**").append(queryFilter.getName()).append("**").append(" ")
                        .append(queryFilter.getOperator().getValue()).append(" ")
                        .append(queryFilter.getValue()).append(" ");
            }
        }
        parseInfo.setTextInfo(textBuilder.toString());
    }
}
