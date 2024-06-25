package com.tencent.supersonic.chat.server.agent;


import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tencent.supersonic.common.config.LLMConfig;
import com.tencent.supersonic.common.pojo.RecordInfo;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
/*
* 处理代理相关的信息和操作。
* */
@Data
public class Agent extends RecordInfo {

    private Integer id;
    private Integer enableSearch;
    private String name;
    private String description;


    /**
     * 0 offline, 1 online
     */
    private Integer status;
    private List<String> examples;
    private String agentConfig;
    private LLMConfig llmConfig;
    private MultiTurnConfig multiTurnConfig;

    public List<String> getTools(AgentToolType type) {
        /*
        * 将agentConfig字段解析为一个Map，
        * 然后从这个Map中获取"tools"键对应的值（一个列表）。
        * 过滤这个列表，只保留那些其"type"键对应的值与输入参数的名称相同的元素，
        * 最后将这些元素转换为JSON字符串并返回*/
        Map map = JSONObject.parseObject(agentConfig, Map.class);
        if (CollectionUtils.isEmpty(map) || map.get("tools") == null) {
            return Lists.newArrayList();
        }
        List<Map> toolList = (List) map.get("tools");
        return toolList.stream()
                .filter(tool -> {
                            if (Objects.isNull(type)) {
                                return true;
                            }
                            return type.name().equals(tool.get("type"));
                        }
                )
                .map(JSONObject::toJSONString)
                .collect(Collectors.toList());
    }

    public boolean enableSearch() {
        return enableSearch != null && enableSearch == 1;
    }
    /*
    * 这个方法返回一个布尔值，表示是否启用搜索。如果enableSearch字段不为null且等于1，则返回true，否则返回false。*/
    public static boolean containsAllModel(Set<Long> detectViewIds) {
        /*
        * 接收一个Long类型的Set作为参数，并返回一个布尔值。如果输入的Set不为空且包含-1L，则返回true，否则返回false。
        */
        return !CollectionUtils.isEmpty(detectViewIds) && detectViewIds.contains(-1L);
    }

    public List<NL2SQLTool> getParserTools(AgentToolType agentToolType) {
        /*
        * 这个方法接收一个AgentToolType类型的参数，并返回一个NL2SQLTool类型的列表。它首先调用getTools方法获取工具列表，然后将这些工具解析为NL2SQLTool对象并返回。
        * */
        List<String> tools = this.getTools(agentToolType);
        if (CollectionUtils.isEmpty(tools)) {
            return Lists.newArrayList();
        }
        return tools.stream().map(tool -> JSONObject.parseObject(tool, NL2SQLTool.class))
                .collect(Collectors.toList());
    }
    /*containsLLMParserTool()，containsRuleTool()，containsNL2SQLTool()：这些方法返回一个布尔值，表示是否包含特定类型的工具。它们通过调用getParserTools方法并检查返回的列表是否为空来实现。*/
    public boolean containsLLMParserTool() {

        return !CollectionUtils.isEmpty(getParserTools(AgentToolType.NL2SQL_LLM));
    }

    public boolean containsRuleTool() {
        return !CollectionUtils.isEmpty(getParserTools(AgentToolType.NL2SQL_RULE));
    }

    public boolean containsNL2SQLTool() {
        return !CollectionUtils.isEmpty(getParserTools(AgentToolType.NL2SQL_LLM))
                || !CollectionUtils.isEmpty(getParserTools(AgentToolType.NL2SQL_RULE));
    }

    /*getDataSetIds()和getDataSetIds(AgentToolType agentToolType)：这些方法返回一个包含数据集ID的Set。它们通过调用getParserTools方法获取工具列表，然后从这些工具中获取数据集ID并返回。如果输入的Set包含所有模型（即包含-1L），则返回一个空的Set
    * */
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
                .filter(modelIds -> !CollectionUtils.isEmpty(modelIds))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
