package com.tencent.supersonic.knowledge.service;

import com.tencent.supersonic.knowledge.utils.NatureHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Data
@Service
public class LoadRemoveService {

    @Value("${mapper.remove.agentId:}")
    private Integer mapperRemoveAgentId;

    @Value("${mapper.remove.nature.prefix:}")
    private String mapperRemoveNaturePrefix;

    public List removeNatures(List value, Integer agentId, Set<Long> detectModelIds) {
        if (CollectionUtils.isEmpty(value)) {
            return value;
        }
        List<String> resultList = new ArrayList<>(value);
        if (!CollectionUtils.isEmpty(detectModelIds)) {
            resultList.removeIf(nature -> {
                if (Objects.isNull(nature)) {
                    return false;
                }
                Long modelId = NatureHelper.getModelId(nature);
                if (Objects.nonNull(modelId)) {
                    return !detectModelIds.contains(modelId);
                }
                return false;
            });
        }
        if (Objects.nonNull(mapperRemoveAgentId)
                && mapperRemoveAgentId.equals(agentId)
                && StringUtils.isNotBlank(mapperRemoveNaturePrefix)) {
            resultList.removeIf(nature -> {
                if (Objects.isNull(nature)) {
                    return false;
                }
                return nature.startsWith(mapperRemoveNaturePrefix);
            });
        }
        return resultList;
    }

}
