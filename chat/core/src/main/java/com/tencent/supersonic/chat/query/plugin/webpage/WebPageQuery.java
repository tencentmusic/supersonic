package com.tencent.supersonic.chat.query.plugin.webpage;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.ModelSchema;
import com.tencent.supersonic.chat.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.chat.api.pojo.SchemaElementType;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilter;
import com.tencent.supersonic.chat.api.pojo.request.QueryFilters;
import com.tencent.supersonic.chat.api.pojo.request.QueryReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.ParamOption;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.query.plugin.WebBase;
import com.tencent.supersonic.chat.query.plugin.WebBaseResult;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Component
public class WebPageQuery extends PluginSemanticQuery {

    public static String QUERY_MODE = "WEB_PAGE";

    public WebPageQuery() {
        QueryManager.register(this);
    }

    @Override
    public String getQueryMode() {
        return QUERY_MODE;
    }

    @Override
    public QueryResult execute(User user) {
        QueryResult queryResult = new QueryResult();
        queryResult.setQueryMode(QUERY_MODE);
        Map<String, Object> properties = parseInfo.getProperties();
        PluginParseResult pluginParseResult = JsonUtil.toObject(JsonUtil.toString(properties.get(Constants.CONTEXT)),
                PluginParseResult.class);
        WebPageResponse webPageResponse = buildResponse(pluginParseResult);
        queryResult.setResponse(webPageResponse);
        SemanticService semanticService = ContextUtils.getBean(SemanticService.class);
        ModelSchema modelSchema = semanticService.getModelSchema(parseInfo.getModelId());
        parseInfo.setModel(modelSchema.getModel());
        queryResult.setQueryState(QueryState.SUCCESS);
        return queryResult;
    }

    protected WebPageResponse buildResponse(PluginParseResult pluginParseResult) {
        Plugin plugin = pluginParseResult.getPlugin();
        WebPageResponse webPageResponse = new WebPageResponse();
        webPageResponse.setName(plugin.getName());
        webPageResponse.setPluginId(plugin.getId());
        webPageResponse.setPluginType(plugin.getType());
        WebBase webPage = JsonUtil.toObject(plugin.getConfig(), WebBase.class);
        WebBaseResult webBaseResult = buildWebPageResult(webPage, pluginParseResult);
        webPageResponse.setWebPage(webBaseResult);
        return webPageResponse;
    }

    private WebBaseResult buildWebPageResult(WebBase webPage, PluginParseResult pluginParseResult) {
        WebBaseResult webBaseResult = new WebBaseResult();
        webBaseResult.setUrl(webPage.getUrl());
        Map<String, Object> elementValueMap = getElementMap(pluginParseResult);
        List<ParamOption> paramOptions = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(webPage.getParamOptions()) && !CollectionUtils.isEmpty(elementValueMap)) {
            for (ParamOption paramOption : webPage.getParamOptions()) {
                if (paramOption.getModelId() != null && !paramOption.getModelId().equals(parseInfo.getModelId())) {
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
        webBaseResult.setParams(paramOptions);
        return webBaseResult;
    }

    protected Map<String, Object> getElementMap(PluginParseResult pluginParseResult) {
        Map<String, Object> elementValueMap = new HashMap<>();
        Map<Long, Object> filterValueMap = getFilterMap(pluginParseResult);
        List<SchemaElementMatch> schemaElementMatchList = parseInfo.getElementMatches();
        if (!CollectionUtils.isEmpty(schemaElementMatchList)) {
            schemaElementMatchList.stream()
                    .filter(schemaElementMatch ->
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

}
