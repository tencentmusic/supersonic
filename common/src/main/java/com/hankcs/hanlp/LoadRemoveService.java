package com.hankcs.hanlp;

import com.tencent.supersonic.common.pojo.enums.DictWordType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Data
@Slf4j
public class LoadRemoveService {

    @Value("${mapper.remove.nature.prefix:}")
    private String mapperRemoveNaturePrefix;

    public List removeNatures(List value, Set<Long> detectModelIds) {
        if (CollectionUtils.isEmpty(value)) {
            return value;
        }
        List<String> resultList = new ArrayList<>(value);
        if (!CollectionUtils.isEmpty(detectModelIds)) {
            resultList.removeIf(nature -> {
                if (Objects.isNull(nature)) {
                    return false;
                }
                Long modelId = getDataSetId(nature);
                if (Objects.nonNull(modelId)) {
                    return !detectModelIds.contains(modelId);
                }
                return false;
            });
        }
        if (StringUtils.isNotBlank(mapperRemoveNaturePrefix)) {
            resultList.removeIf(nature -> {
                if (Objects.isNull(nature)) {
                    return false;
                }
                return nature.startsWith(mapperRemoveNaturePrefix);
            });
        }
        return resultList;
    }

    public Long getDataSetId(String nature) {
        try {
            String[] split = nature.split(DictWordType.NATURE_SPILT);
            if (split.length <= 1) {
                return null;
            }
            return Long.valueOf(split[1]);
        } catch (NumberFormatException e) {
            log.error("", e);
        }
        return null;
    }

}
