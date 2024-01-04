package com.tencent.supersonic.chat.core.agent;


import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.RecordInfo;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.util.CollectionUtils;

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

    public List<String> getTools(AgentToolType type) {
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

    public static boolean containsAllModel(Set<Long> detectModelIds) {
        return !CollectionUtils.isEmpty(detectModelIds) && detectModelIds.contains(-1L);
    }

    public List<NL2SQLTool> getParserTools(AgentToolType agentToolType) {
        List<String> tools = this.getTools(agentToolType);
        if (CollectionUtils.isEmpty(tools)) {
            return Lists.newArrayList();
        }
        return tools.stream().map(tool -> JSONObject.parseObject(tool, NL2SQLTool.class))
                .collect(Collectors.toList());
    }

    public Set<Long> getModelIds(AgentToolType agentToolType) {
        List<NL2SQLTool> commonAgentTools = getParserTools(agentToolType);
        if (CollectionUtils.isEmpty(commonAgentTools)) {
            return new HashSet<>();
        }
        return commonAgentTools.stream().map(NL2SQLTool::getModelIds)
                .filter(modelIds -> !CollectionUtils.isEmpty(modelIds))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
