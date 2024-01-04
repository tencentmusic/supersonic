package com.tencent.supersonic.headless.core.manager;


import com.tencent.supersonic.headless.api.enums.IdentifyType;
import com.tencent.supersonic.headless.api.response.DimensionResp;
import com.tencent.supersonic.headless.core.pojo.yaml.DimensionYamlTpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
public class DimensionYamlManager {

    public static List<DimensionYamlTpl> convert2DimensionYaml(List<DimensionResp> dimensions) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return new ArrayList<>();
        }
        return dimensions.stream()
                .filter(dimension -> !dimension.getType().equalsIgnoreCase(IdentifyType.primary.name()))
                .map(DimensionYamlManager::convert2DimensionYamlTpl).collect(Collectors.toList());
    }

    public static DimensionYamlTpl convert2DimensionYamlTpl(DimensionResp dimension) {
        DimensionYamlTpl dimensionYamlTpl = new DimensionYamlTpl();
        BeanUtils.copyProperties(dimension, dimensionYamlTpl);
        dimensionYamlTpl.setName(dimension.getBizName());
        dimensionYamlTpl.setOwners(dimension.getCreatedBy());
        return dimensionYamlTpl;
    }

}
