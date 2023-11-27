package com.tencent.supersonic.chat.agent.tool;


import lombok.Data;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

@Data
public class RuleQueryTool extends CommonAgentTool {


    private List<String> queryModes;

    private List<String> queryTypes;

    public boolean isContainsAllModel() {
        return CollectionUtils.isNotEmpty(modelIds) && modelIds.contains(-1L);
    }

}
