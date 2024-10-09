package com.tencent.supersonic.chat.server.agent;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.common.pojo.enums.ChatModelType;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class Agent extends RecordInfo {

    private Integer id;
    private String name;
    private String description;
    /** 0 offline, 1 online */
    private Integer status;
    private List<String> examples;
    private Integer enableSearch;
    private Integer enableMemoryReview;
    private String toolConfig;
    private Map<ChatModelType, Integer> chatModelConfig = Collections.EMPTY_MAP;
    private PromptConfig promptConfig;
    private MultiTurnConfig multiTurnConfig;
    private VisualConfig visualConfig;

    public List<String> getTools(AgentToolType type) {
        Map map = JSONObject.parseObject(toolConfig, Map.class);
        if (CollectionUtils.isEmpty(map) || map.get("tools") == null) {
            return Lists.newArrayList();
        }
        List<Map> toolList = (List) map.get("tools");
        return toolList.stream().filter(tool -> {
            if (Objects.isNull(type)) {
                return true;
            }
            return type.name().equals(tool.get("type"));
        }).map(JSONObject::toJSONString).collect(Collectors.toList());
    }

    public boolean enableSearch() {
        return enableSearch != null && enableSearch == 1;
    }

    public boolean enableMemoryReview() {
        return enableMemoryReview != null && enableMemoryReview == 1;
    }

    public static boolean containsAllModel(Set<Long> detectViewIds) {
        return !CollectionUtils.isEmpty(detectViewIds) && detectViewIds.contains(-1L);
    }

    public List<NL2SQLTool> getParserTools(AgentToolType agentToolType) {
        List<String> tools = this.getTools(agentToolType);
        if (CollectionUtils.isEmpty(tools)) {
            return Lists.newArrayList();
        }
        return tools.stream().map(tool -> JSONObject.parseObject(tool, NL2SQLTool.class))
                .collect(Collectors.toList());
    }

    public boolean containsPluginTool() {
        return !CollectionUtils.isEmpty(getParserTools(AgentToolType.PLUGIN));
    }

    public boolean containsLLMTool() {
        return !CollectionUtils.isEmpty(getParserTools(AgentToolType.NL2SQL_LLM));
    }

    public boolean containsRuleTool() {
        return !CollectionUtils.isEmpty(getParserTools(AgentToolType.NL2SQL_RULE));
    }

    public boolean containsNL2SQLTool() {
        return !CollectionUtils.isEmpty(getParserTools(AgentToolType.NL2SQL_LLM))
                || !CollectionUtils.isEmpty(getParserTools(AgentToolType.NL2SQL_RULE));
    }

    public boolean containsAnyTool() {
        Map map = JSONObject.parseObject(toolConfig, Map.class);
        if (CollectionUtils.isEmpty(map)) {
            return false;
        }
        List<Map> toolList = (List) map.get("tools");
        if (CollectionUtils.isEmpty(toolList)) {
            return false;
        }

        return true;
    }

    public Set<Long> getDataSetIds() {
        Set<Long> dataSetIds = getDataSetIds(null);
        if (containsAllModel(dataSetIds)) {
            return Sets.newHashSet();
        }
        return dataSetIds;
    }

    public Set<Long> getDataSetIds(AgentToolType agentToolType) {
        List<NL2SQLTool> commonAgentTools = getParserTools(agentToolType);
        if (CollectionUtils.isEmpty(commonAgentTools)) {
            return new HashSet<>();
        }
        return commonAgentTools.stream().map(NL2SQLTool::getDataSetIds)
                .filter(modelIds -> !CollectionUtils.isEmpty(modelIds)).flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
