package com.hankcs.hanlp;

import com.tencent.supersonic.common.pojo.enums.DictWordType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Data
@Slf4j
public class LoadRemoveService {

    public List removeNatures(List value, Set<Long> modelIdOrDataSetIds) {
        if (CollectionUtils.isEmpty(value)) {
            return value;
        }
        List<String> resultList = new ArrayList<>(value);
        if (!CollectionUtils.isEmpty(modelIdOrDataSetIds)) {
            resultList.removeIf(nature -> {
                if (Objects.isNull(nature)) {
                    return false;
                }
                Long id = getId(nature);
                if (Objects.nonNull(id)) {
                    return !modelIdOrDataSetIds.contains(id);
                }
                return false;
            });
        }
        return resultList;
    }

    public Long getId(String nature) {
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
