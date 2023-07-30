package com.tencent.supersonic.chat.query.plugin.webpage;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.*;
import com.tencent.supersonic.chat.api.pojo.response.EntityInfo;
import com.tencent.supersonic.chat.api.pojo.response.QueryResult;
import com.tencent.supersonic.chat.api.pojo.response.QueryState;
import com.tencent.supersonic.chat.config.ChatConfigRich;
import com.tencent.supersonic.chat.plugin.Plugin;
import com.tencent.supersonic.chat.query.QueryManager;
import com.tencent.supersonic.chat.query.plugin.PluginSemanticQuery;
import com.tencent.supersonic.chat.query.plugin.WebBase;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.service.SemanticService;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Plugin plugin = (Plugin) properties.get(Constants.CONTEXT);
        WebPageResponse webPageResponse = buildResponse(plugin);
        queryResult.setResponse(webPageResponse);
        if (parseInfo.getDomainId() != null && parseInfo.getDomainId() > 0
                && parseInfo.getEntity() != null && parseInfo.getEntity() > 0) {
            ChatConfigRich chatConfigRichResp = configService.getConfigRichInfo(parseInfo.getDomainId());
            updateSemanticParse(chatConfigRichResp, parseInfo.getEntity());
            EntityInfo entityInfo = ContextUtils.getBean(SemanticService.class).getEntityInfo(parseInfo, user);
            queryResult.setEntityInfo(entityInfo);
        } else {
            queryResult.setEntityInfo(null);
        }
        queryResult.setQueryState(QueryState.SUCCESS);
        return queryResult;
    }

    private void updateSemanticParse(ChatConfigRich chatConfigRichResp, Long entityId) {
        parseInfo.setEntity(entityId);
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
        fillWebPage(webPage);
        webPageResponse.setWebPage(webPage);
        return webPageResponse;
    }

    private void fillWebPage(WebBase webPage) {
        List<SchemaElementMatch> schemaElementMatchList = parseInfo.getElementMatches();
        Map<String, Object> elementValueMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(schemaElementMatchList) && !CollectionUtils.isEmpty(webPage.getParams()) ) {
            schemaElementMatchList.stream()
                    .filter(schemaElementMatch ->
                            SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType()))
                    .sorted(Comparator.comparingDouble(SchemaElementMatch::getSimilarity))
                    .forEach(schemaElementMatch ->
                            elementValueMap.put(String.valueOf(schemaElementMatch.getElement().getId()),
                                    schemaElementMatch.getWord()));
        }
        if (!CollectionUtils.isEmpty(parseInfo.getDimensionFilters())) {
            parseInfo.getDimensionFilters().forEach(
                    filter -> elementValueMap.put(String.valueOf(filter.getElementID()), filter.getValue())
            );
        }
        Map<String, Object> params = webPage.getParams();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            String elementId = String.valueOf(entry.getValue());
            Object elementValue = elementValueMap.get(elementId);
            webPage.getValueParams().put(key, elementValue);
        }
    }

}
