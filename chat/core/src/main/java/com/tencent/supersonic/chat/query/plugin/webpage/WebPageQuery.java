package com.tencent.supersonic.chat.query.plugin.webpage;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.plugin.PluginParseResult;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.ParamOption;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.query.plugin.WebBase;
import com.tencent.supersonic.chat.query.plugin.WebBaseResult;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

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
        ConfigService configService = ContextUtils.getBean(ConfigService.class);
        QueryResult queryResult = new QueryResult();
        queryResult.setQueryMode(QUERY_MODE);
        Map<String, Object> properties = parseInfo.getProperties();
        PluginParseResult pluginParseResult = JsonUtil.toObject(JsonUtil.toString(properties.get(Constants.CONTEXT)), PluginParseResult.class);
        WebPageResponse webPageResponse = buildResponse(pluginParseResult.getPlugin());
        queryResult.setResponse(webPageResponse);
        if (parseInfo.getDomainId() != null && parseInfo.getDomainId() > 0
                && parseInfo.getEntity() != null && Objects.nonNull(parseInfo.getEntity().getId())
                && parseInfo.getEntity().getId() > 0) {
            ChatConfigRichResp chatConfigRichResp = configService.getConfigRichInfo(parseInfo.getDomainId());
            updateSemanticParse(chatConfigRichResp);
            EntityInfo entityInfo = ContextUtils.getBean(SemanticService.class).getEntityInfo(parseInfo, user);
            queryResult.setEntityInfo(entityInfo);
        } else {
            queryResult.setEntityInfo(null);
        }
        queryResult.setQueryState(QueryState.SUCCESS);
        return queryResult;
    }

    private void updateSemanticParse(ChatConfigRichResp chatConfigRichResp) {
        SchemaElement domain = new SchemaElement();
        domain.setId(chatConfigRichResp.getDomainId());
        domain.setName(chatConfigRichResp.getDomainName());
        parseInfo.setDomain(domain);
    }

    protected WebPageResponse buildResponse(Plugin plugin) {
        WebPageResponse webPageResponse = new WebPageResponse();
        webPageResponse.setName(plugin.getName());
        webPageResponse.setPluginId(plugin.getId());
        webPageResponse.setPluginType(plugin.getType());
        WebBase webPage = JsonUtil.toObject(plugin.getConfig(), WebBase.class);
        WebBaseResult webBaseResult = buildWebPageResult(webPage);
        webPageResponse.setWebPage(webBaseResult);
        return webPageResponse;
    }

    private WebBaseResult buildWebPageResult(WebBase webPage) {
        WebBaseResult webBaseResult = new WebBaseResult();
        webBaseResult.setUrl(webPage.getUrl());
        Map<String, Object> elementValueMap = getElementMap();
        if (!CollectionUtils.isEmpty(webPage.getParamOptions()) && !CollectionUtils.isEmpty(elementValueMap)) {
            for (ParamOption paramOption : webPage.getParamOptions()) {
                if (!ParamOption.ParamType.SEMANTIC.equals(paramOption.getParamType())) {
                    continue;
                }
                String elementId = String.valueOf(paramOption.getElementId());
                Object elementValue = elementValueMap.get(elementId);
                paramOption.setValue(elementValue);
            }
        }
        webBaseResult.setParams(webPage.getParamOptions());
        return webBaseResult;
    }

    private Map<String, Object> getElementMap() {
        Map<String, Object> elementValueMap = new HashMap<>();
        List<SchemaElementMatch> schemaElementMatchList = parseInfo.getElementMatches();
        if (!CollectionUtils.isEmpty(schemaElementMatchList)) {
            schemaElementMatchList.stream()
                    .filter(schemaElementMatch ->
                            SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType())
                                    || SchemaElementType.ID.equals(schemaElementMatch.getElement().getType()))
                    .sorted(Comparator.comparingDouble(SchemaElementMatch::getSimilarity))
                    .forEach(schemaElementMatch ->
                            elementValueMap.put(String.valueOf(schemaElementMatch.getElement().getId()),
                                    schemaElementMatch.getWord()));
        }
        return elementValueMap;
    }

}
