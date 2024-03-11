package com.tencent.supersonic.chat.server.plugin.recall;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.server.plugin.Plugin;
import com.tencent.supersonic.chat.server.plugin.PluginManager;
import com.tencent.supersonic.chat.server.plugin.PluginParseResult;
import com.tencent.supersonic.chat.server.plugin.PluginRecallResult;
import com.tencent.supersonic.chat.server.plugin.build.PluginSemanticQuery;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.common.pojo.enums.FilterOperatorEnum;
import com.tencent.supersonic.headless.api.pojo.SchemaElementMatch;
import com.tencent.supersonic.headless.api.pojo.SchemaElementType;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilter;
import com.tencent.supersonic.headless.api.pojo.request.QueryFilters;
import com.tencent.supersonic.headless.core.pojo.QueryContext;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PluginParser defines the basic process and common methods for recalling plugins.
 */
public abstract class PluginParser {

    public void parse(ChatParseReq chatParseReq) {
        if (!checkPreCondition(chatParseReq)) {
            return;
        }
        PluginRecallResult pluginRecallResult = recallPlugin(chatParseReq);
        if (pluginRecallResult == null) {
            return;
        }
        buildQuery(chatParseReq, pluginRecallResult);
    }

    public abstract boolean checkPreCondition(ChatParseReq chatParseReq);

    public abstract PluginRecallResult recallPlugin(ChatParseReq chatParseReq);

    public void buildQuery(ChatParseReq chatParseReq, PluginRecallResult pluginRecallResult) {
        Plugin plugin = pluginRecallResult.getPlugin();
        Set<Long> dataSetIds = pluginRecallResult.getDataSetIds();
        if (plugin.isContainsAllModel()) {
            dataSetIds = Sets.newHashSet(-1L);
        }
        for (Long dataSetId : dataSetIds) {
            //todo
            PluginSemanticQuery pluginQuery = null;
            SemanticParseInfo semanticParseInfo = buildSemanticParseInfo(dataSetId, plugin,
                    null, pluginRecallResult.getDistance());
            semanticParseInfo.setQueryMode(pluginQuery.getQueryMode());
            semanticParseInfo.setScore(pluginRecallResult.getScore());
            pluginQuery.setParseInfo(semanticParseInfo);
            //chatParseReq.getCandidateQueries().add(pluginQuery);
        }
    }

    protected List<Plugin> getPluginList(ChatParseReq chatParseReq) {
        return PluginManager.getPluginAgentCanSupport(chatParseReq);
    }

    protected SemanticParseInfo buildSemanticParseInfo(Long dataSetId, Plugin plugin,
                                                       QueryContext queryContext, double distance) {
        List<SchemaElementMatch> schemaElementMatches = queryContext.getMapInfo().getMatchedElements(dataSetId);
        QueryFilters queryFilters = queryContext.getQueryFilters();
        if (dataSetId == null && !CollectionUtils.isEmpty(plugin.getDataSetList())) {
            dataSetId = plugin.getDataSetList().get(0);
        }
        if (schemaElementMatches == null) {
            schemaElementMatches = Lists.newArrayList();
        }
        SemanticParseInfo semanticParseInfo = new SemanticParseInfo();
        semanticParseInfo.setElementMatches(schemaElementMatches);
        semanticParseInfo.setDataSet(queryContext.getSemanticSchema().getDataSet(dataSetId));
        Map<String, Object> properties = new HashMap<>();
        PluginParseResult pluginParseResult = new PluginParseResult();
        pluginParseResult.setPlugin(plugin);
        pluginParseResult.setQueryFilters(queryFilters);
        pluginParseResult.setDistance(distance);
        pluginParseResult.setQueryText(queryContext.getQueryText());
        properties.put(Constants.CONTEXT, pluginParseResult);
        properties.put("type", "plugin");
        properties.put("name", plugin.getName());
        semanticParseInfo.setProperties(properties);
        semanticParseInfo.setScore(distance);
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
