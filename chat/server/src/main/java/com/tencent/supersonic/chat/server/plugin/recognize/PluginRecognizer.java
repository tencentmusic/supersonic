package com.tencent.supersonic.chat.server.plugin.recognize;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.server.plugin.Plugin;
import com.tencent.supersonic.chat.server.plugin.PluginManager;
import com.tencent.supersonic.chat.server.plugin.PluginParseResult;
import com.tencent.supersonic.chat.server.plugin.PluginRecallResult;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.SchemaElement;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import com.tencent.supersonic.headless.api.pojo.response.ParseResp;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PluginParser defines the basic process and common methods for recalling plugins.
 */
public abstract class PluginRecognizer {

    public void recognize(ChatParseContext chatParseContext, ParseResp parseResp) {
        if (!checkPreCondition(chatParseContext)) {
            return;
        }
        PluginRecallResult pluginRecallResult = recallPlugin(chatParseContext);
        if (pluginRecallResult == null) {
            return;
        }
        buildQuery(chatParseContext, parseResp, pluginRecallResult);
    }

    public abstract boolean checkPreCondition(ChatParseContext chatParseContext);

    public abstract PluginRecallResult recallPlugin(ChatParseContext chatParseContext);

    public void buildQuery(ChatParseContext chatParseContext, ParseResp parseResp,
                           PluginRecallResult pluginRecallResult) {
        Plugin plugin = pluginRecallResult.getPlugin();
        Set<Long> dataSetIds = pluginRecallResult.getDataSetIds();
        if (plugin.isContainsAllModel()) {
            dataSetIds = Sets.newHashSet(-1L);
        }
        for (Long dataSetId : dataSetIds) {
            SemanticParseInfo semanticParseInfo = buildSemanticParseInfo(dataSetId, plugin,
                    chatParseContext, pluginRecallResult.getDistance());
            semanticParseInfo.setQueryMode(plugin.getType());
            semanticParseInfo.setScore(pluginRecallResult.getScore());
            parseResp.getSelectedParses().add(semanticParseInfo);
        }
    }

    protected List<Plugin> getPluginList(ChatParseContext chatParseContext) {
        return PluginManager.getPluginAgentCanSupport(chatParseContext);
    }

    protected SemanticParseInfo buildSemanticParseInfo(Long dataSetId, Plugin plugin,
                                                       ChatParseContext chatParseContext, double distance) {
        List<SchemaElementMatch> schemaElementMatches = chatParseContext.getMapInfo().getMatchedElements(dataSetId);
        QueryFilters queryFilters = chatParseContext.getQueryFilters();
        if (schemaElementMatches == null) {
            schemaElementMatches = Lists.newArrayList();
        }
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setElementMatches(schemaElementMatches);
        SchemaElement schemaElement = new SchemaElement();
        schemaElement.setDataSet(dataSetId);
        semanticParseInfo.setDataSet(schemaElement);
        Map<String, Object> properties = new HashMap<>();
        PluginParseResult pluginParseResult = new PluginParseResult();
        pluginParseResult.setPlugin(plugin);
        pluginParseResult.setQueryFilters(queryFilters);
        pluginParseResult.setDistance(distance);
        pluginParseResult.setQueryText(chatParseContext.getQueryText());
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
        schemaElementMatches.stream().filter(schemaElementMatch ->
                        SchemaElementType.VALUE.equals(schemaElementMatch.getElement().getType())
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
