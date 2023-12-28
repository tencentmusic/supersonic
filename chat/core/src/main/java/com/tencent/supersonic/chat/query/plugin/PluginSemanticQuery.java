package com.tencent.supersonic.chat.query.plugin;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.BaseSemanticQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class PluginSemanticQuery extends BaseSemanticQuery {

    @Override
    public String explain(User user) {
        return null;
    }

    @Override
    public void initS2Sql(User user) {

    }

    private Map<Long, Object> getFilterMap(PluginParseResult pluginParseResult) {
        Map<Long, Object> map = new HashMap<>();
        QueryReq queryReq = pluginParseResult.getRequest();
        if (queryReq == null || queryReq.getQueryFilters() == null) {
            return map;
        }
        QueryFilters queryFilters = queryReq.getQueryFilters();
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
        List<SchemaElementMatch> schemaElementMatchList = parseInfo.getElementMatches();
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
                if (paramOption.getModelId() != null
                        && !parseInfo.getModel().getModelIds().contains(paramOption.getModelId())) {
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

}
