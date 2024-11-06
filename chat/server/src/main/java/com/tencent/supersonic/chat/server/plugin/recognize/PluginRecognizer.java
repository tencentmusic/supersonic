package com.tencent.supersonic.chat.server.plugin.recognize;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.api.pojo.response.ChatParseResp;
import com.tencent.supersonic.chat.server.plugin.ChatPlugin;
import com.tencent.supersonic.chat.server.plugin.PluginManager;
import com.tencent.supersonic.chat.server.plugin.PluginParseResult;
import com.tencent.supersonic.chat.server.plugin.PluginRecallResult;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.chat.server.util.QueryReqConverter;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SchemaMapInfo;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import com.tencent.supersonic.headless.server.facade.service.ChatLayerService;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** PluginParser defines the basic process and common methods for recalling plugins. */
public abstract class PluginRecognizer {

    public void recognize(ParseContext parseContext) {
        if (!checkPreCondition(parseContext)) {
            return;
        }
        PluginRecallResult pluginRecallResult = recallPlugin(parseContext);
        if (pluginRecallResult == null) {
            return;
        }
        buildQuery(parseContext, parseContext.getResponse(), pluginRecallResult);
    }

    public abstract boolean checkPreCondition(ParseContext parseContext);

    public abstract PluginRecallResult recallPlugin(ParseContext parseContext);

    public void buildQuery(ParseContext parseContext, ChatParseResp parseResp,
            PluginRecallResult pluginRecallResult) {
        ChatPlugin plugin = pluginRecallResult.getPlugin();
        Set<Long> dataSetIds = pluginRecallResult.getDataSetIds();
        if (plugin.isContainsAllDataSet()) {
            dataSetIds = Sets.newHashSet(-1L);
        }
        ChatLayerService chatLayerService = ContextUtils.getBean(ChatLayerService.class);
        QueryNLReq queryNLReq = QueryReqConverter.buildQueryNLReq(parseContext);
        SchemaMapInfo schemaMapInfo = chatLayerService.map(queryNLReq).getMapInfo();
        int parseId = 1;
        for (Long dataSetId : dataSetIds) {
            SemanticParseInfo semanticParseInfo = buildSemanticParseInfo(dataSetId, plugin,
                    parseContext, schemaMapInfo, pluginRecallResult.getDistance());
            semanticParseInfo.setId(parseId++);
            semanticParseInfo.setQueryMode(plugin.getType());
            semanticParseInfo.setScore(pluginRecallResult.getScore());
            parseResp.getSelectedParses().add(semanticParseInfo);
        }
    }

    protected List<ChatPlugin> getPluginList(ParseContext parseContext) {
        return PluginManager.getPluginAgentCanSupport(parseContext);
    }

    protected SemanticParseInfo buildSemanticParseInfo(Long dataSetId, ChatPlugin plugin,
            ParseContext parseContext, SchemaMapInfo mapInfo, double distance) {
        List<SchemaElementMatch> schemaElementMatches = mapInfo.getMatchedElements(dataSetId);
        QueryFilters queryFilters = parseContext.getRequest().getQueryFilters();
        if (schemaElementMatches == null) {
            schemaElementMatches = Lists.newArrayList();
        }
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setElementMatches(schemaElementMatches);
        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setDataSetId(dataSetId);
        semanticParseInfo.setDataSet(schemaElement);
        Map<String, Object> properties = new HashMap<>();
        PluginParseResult pluginParseResult = new PluginParseResult();
        pluginParseResult.setPlugin(plugin);
        pluginParseResult.setQueryFilters(queryFilters);
        pluginParseResult.setDistance(distance);
        pluginParseResult.setQueryText(parseContext.getRequest().getQueryText());
        properties.put(Constants.CONTEXT, pluginParseResult);
        properties.put("type", "plugin");
        properties.put("name", plugin.getName());
        semanticParseInfo.setProperties(properties);
        semanticParseInfo.setScore(distance);
        semanticParseInfo.setTextInfo(String.format("将由插件工具**%s**来解答", plugin.getName()));
        fillSemanticParseInfo(semanticParseInfo);
        return semanticParseInfo;
    }

    private void fillSemanticParseInfo(SemanticParseInfo semanticParseInfo) {
        List<SchemaElementMatch> schemaElementMatches = semanticParseInfo.getElementMatches();
        if (CollectionUtils.isEmpty(schemaElementMatches)) {
            return;
        }
        schemaElementMatches.stream()
                .filter(schemaElementMatch -> SchemaElementType.VALUE
                        .equals(schemaElementMatch.getElement().getType())
                        || SchemaElementType.ID.equals(schemaElementMatch.getElement().getType()))
                .forEach(schemaElementMatch -> {
                    QueryFilter queryFilter = new QueryFilter();
                    queryFilter.setValue(schemaElementMatch.getWord());
                    queryFilter.setElementID(schemaElementMatch.getElement().getId());
                    queryFilter.setName(schemaElementMatch.getElement().getName());
                    queryFilter.setOperator(FilterOperatorEnum.EQUALS);
                    queryFilter.setBizName(schemaElementMatch.getElement().getBizName());
                    semanticParseInfo.getDimensionFilters().add(queryFilter);
                });
    }
}
