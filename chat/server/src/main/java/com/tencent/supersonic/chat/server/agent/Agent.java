package com.tencent.supersonic.chat.server.agent;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.server.memory.MemoryReviewTask;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.common.pojo.User;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class Agent extends RecordInfo {

    private static final int ONLINE_STATUS = 1;
    private static final int OFFLINE_STATUS = 0;
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    private Integer id;
    private String name;
    private String description;
    /** 0 offline, 1 online */
    private Integer status = ONLINE_STATUS;
    private List<String> examples;
    private Integer enableSearch = ENABLED;
    private Integer enableFeedback = DISABLED;
    private String toolConfig;
    private Map<String, ChatApp> chatAppConfig = Collections.emptyMap();
    private VisualConfig visualConfig;
    private List<String> admins = Lists.newArrayList();
    private List<String> viewers = Lists.newArrayList();
    private List<String> adminOrgs = Lists.newArrayList();
    private List<String> viewOrgs = Lists.newArrayList();
    private Integer isOpen = 0;

    public List<String> getTools(AgentToolType type) {
        Map<String, Object> map = JSONObject.parseObject(toolConfig, Map.class);
        if (CollectionUtils.isEmpty(map) || map.get("tools") == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> toolList = (List<Map<String, Object>>) map.get("tools");
        return toolList.stream()
                .filter(tool -> type == null || type.name().equals(tool.get("type")))
                .map(JSONObject::toJSONString).collect(Collectors.toList());
    }

    public boolean enableSearch() {
        return enableSearch == ENABLED;
    }

    public boolean enableFeedback() {
        return enableFeedback == ENABLED;
    }

    public boolean enableMemoryReview() {
        ChatApp memoryReviewApp = chatAppConfig.get(MemoryReviewTask.APP_KEY);
        return memoryReviewApp != null && memoryReviewApp.isEnable();
    }

    public static boolean containsAllModel(Set<Long> detectViewIds) {
        return !CollectionUtils.isEmpty(detectViewIds) && detectViewIds.contains(-1L);
    }

    public List<DatasetTool> getParserTools(AgentToolType agentToolType) {
        List<String> tools = this.getTools(agentToolType);
        if (CollectionUtils.isEmpty(tools)) {
            return Collections.emptyList();
        }
        return tools.stream().map(tool -> JSONObject.parseObject(tool, DatasetTool.class))
                .collect(Collectors.toList());
    }

    public boolean containsPluginTool() {
        return !CollectionUtils.isEmpty(getParserTools(AgentToolType.PLUGIN));
    }

    public boolean containsDatasetTool() {
        return !CollectionUtils.isEmpty(getParserTools(AgentToolType.DATASET));
    }

    public boolean containsAnyTool() {
        Map<String, Object> map = JSONObject.parseObject(toolConfig, Map.class);
        if (CollectionUtils.isEmpty(map)) {
            return false;
        }
        List<Map<String, Object>> toolList = (List<Map<String, Object>>) map.get("tools");
        return !CollectionUtils.isEmpty(toolList);
    }

    public Set<Long> getDataSetIds() {
        Set<Long> dataSetIds = getDataSetIds(null);
        if (containsAllModel(dataSetIds)) {
            return Collections.emptySet();
        }
        return dataSetIds;
    }

    public Set<Long> getDataSetIds(AgentToolType agentToolType) {
        List<DatasetTool> commonAgentTools = getParserTools(agentToolType);
        if (CollectionUtils.isEmpty(commonAgentTools)) {
            return Collections.emptySet();
        }
        return commonAgentTools.stream().map(DatasetTool::getDataSetIds)
                .filter(dataSetIds -> !CollectionUtils.isEmpty(dataSetIds))
                .flatMap(Collection::stream).collect(Collectors.toSet());
    }

    public boolean contains(User user, Function<Agent, List<String>> list) {
        return list.apply(this).contains(user.getName());
    }

    public boolean openToAll() {
        return isOpen != null && isOpen == 1;
    }

}
