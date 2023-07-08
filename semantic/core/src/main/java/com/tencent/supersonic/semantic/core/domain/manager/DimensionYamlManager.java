package com.tencent.supersonic.semantic.core.domain.manager;


import com.tencent.supersonic.semantic.api.core.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.common.enums.TypeEnums;
import com.tencent.supersonic.common.util.yaml.YamlUtils;
import com.tencent.supersonic.semantic.core.domain.pojo.Dimension;
import com.tencent.supersonic.semantic.core.domain.utils.DimensionConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Slf4j
@Service
public class DimensionYamlManager {


    public static List<DimensionYamlTpl> convert2DimensionYaml(List<Dimension> dimensions) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return new ArrayList<>();
        }
        List<DimensionYamlTpl> dimensionYamlTpls = dimensions.stream()
                .filter(dimension -> !dimension.getType().equalsIgnoreCase("primary"))
                .map(DimensionConverter::convert2DimensionYamlTpl).collect(Collectors.toList());
        return dimensionYamlTpls;
    }


}
