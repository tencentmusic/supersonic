package com.tencent.supersonic.semantic.model.domain.manager;


import com.tencent.supersonic.semantic.api.model.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.model.domain.pojo.Dimension;
import com.tencent.supersonic.semantic.model.domain.utils.DimensionConverter;
import java.util.ArrayList;
import java.util.List;
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
