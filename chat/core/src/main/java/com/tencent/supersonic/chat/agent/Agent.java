package com.tencent.supersonic.chat.agent;


import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.RecordInfo;
import java.util.Objects;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class Agent extends RecordInfo {

    private Integer id;
    private Integer enableSearch;
    private String name;
    private String description;

    //0 offline, 1 online
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

}
