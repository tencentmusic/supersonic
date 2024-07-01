package com.tencent.supersonic.chat.server.plugin.build;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.server.plugin.PluginParseResult;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import com.tencent.supersonic.headless.api.pojo.response.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class PluginSemanticQuery {

    protected SemanticParseInfo parseInfo;

    public abstract QueryResult build();

    private Map<Long, Object> getFilterMap(PluginParseResult pluginParseResult) {
        Map<Long, Object> map = new HashMap<>();
        QueryFilters queryFilters = pluginParseResult.getQueryFilters();
        if (queryFilters == null) {
            return map;
        }
        List<QueryFilter> queryFilterList = queryFilters.getFilters();
        if (CollectionUtils.isEmpty(queryFilterList)) {
            return map;
        }
        for (QueryFilter queryFilter : queryFilterList) {
            map.put(queryFilter.getElementID(), queryFilter.getValue());
        }
        return map;
    }

    protected Map<String, Object> getElementMap(PluginParseResult pluginParseResult) {
        Map<String, Object> elementValueMap = new HashMap<>();
        Map<Long, Object> filterValueMap = getFilterMap(pluginParseResult);
        List<SchemaElementMatch> schemaElementMatchList = parseInfo.getElementMatches()
                .stream().filter(schemaElementMatch -> schemaElementMatch.getFrequency() != null)
                .sorted(Comparator.comparingLong(SchemaElementMatch::getFrequency).reversed())
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(schemaElementMatchList)) {
            schemaElementMatchList.stream().filter(schemaElementMatch ->
                            SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType())
                                    || SchemaElementType.ID.equals(schemaElementMatch.getElement().getType()))
                    .filter(schemaElementMatch -> schemaElementMatch.getSimilarity() == 1.0)
                    .forEach(schemaElementMatch -> {
                        Object queryFilterValue = filterValueMap.get(schemaElementMatch.getElement().getId());
                        if (queryFilterValue != null) {
                            if (String.valueOf(queryFilterValue).equals(String.valueOf(schemaElementMatch.getWord()))) {
                                elementValueMap.put(
                                        String.valueOf(schemaElementMatch.getElement().getId()),
                                        schemaElementMatch.getWord());
                            }
                        } else {
                            elementValueMap.computeIfAbsent(
                                    String.valueOf(schemaElementMatch.getElement().getId()),
                                    k -> schemaElementMatch.getWord());
                        }
                    });
        }
        return elementValueMap;
    }

    protected WebBase fillWebBaseResult(WebBase webPage, PluginParseResult pluginParseResult) {
        WebBase webBaseResult = new WebBase();
        webBaseResult.setUrl(webPage.getUrl());
        Map<String, Object> elementValueMap = getElementMap(pluginParseResult);
        List<ParamOption> paramOptions = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(webPage.getParamOptions()) && !CollectionUtils.isEmpty(elementValueMap)) {
            for (ParamOption paramOption : webPage.getParamOptions()) {
                if (paramOption.getDataSetId() != null
                        && !parseInfo.getDataSetId().equals(paramOption.getDataSetId())) {
                    continue;
                }
                paramOptions.add(paramOption);
                if (!ParamOption.ParamType.SEMANTIC.equals(paramOption.getParamType())) {
                    continue;
                }
                String elementId = String.valueOf(paramOption.getElementId());
                Object elementValue = elementValueMap.get(elementId);
                paramOption.setValue(elementValue);
            }
        }
        webBaseResult.setParamOptions(paramOptions);
        return webBaseResult;
    }

    public void setParseInfo(SemanticParseInfo parseInfo) {
        this.parseInfo = parseInfo;
    }

}
