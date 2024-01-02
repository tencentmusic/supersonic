package com.tencent.supersonic.headless.server.manager;


import com.tencent.supersonic.headless.common.server.enums.IdentifyType;
import com.tencent.supersonic.headless.common.server.response.DimensionResp;
import com.tencent.supersonic.headless.server.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.headless.server.utils.DimensionConverter;
import lombok.extern.slf4j.Slf4j;
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
                .map(DimensionConverter::convert2DimensionYamlTpl).collect(Collectors.toList());
    }

}
