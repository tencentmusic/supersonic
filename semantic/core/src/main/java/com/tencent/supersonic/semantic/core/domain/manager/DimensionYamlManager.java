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


    private YamlManager yamlManager;

    public DimensionYamlManager(YamlManager yamlManager) {
        this.yamlManager = yamlManager;
    }


    public void generateYamlFile(List<Dimension> dimensions, String fullPath, String datasourceBizName)
            throws Exception {
        String yamlStr = convert2YamlStr(dimensions, datasourceBizName);
        log.info("generate yaml str :{} from metric:{} full path:{}", yamlStr, dimensions, fullPath);
        yamlManager.generateYamlFile(yamlStr, fullPath, getYamlName(datasourceBizName));
    }

    public String getYamlName(String name) {
        return String.format("%s_%s", name, TypeEnums.DIMENSION.getName());
    }

    public static String convert2YamlStr(List<Dimension> dimensions, String datasourceBizName) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return "";
        }
        List<DimensionYamlTpl> dimensionYamlTpls = dimensions.stream()
                .filter(dimension -> !dimension.getType().equalsIgnoreCase("primary"))
                .map(DimensionConverter::convert2DimensionYamlTpl).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(dimensionYamlTpls)) {
            return "";
        }
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("source", datasourceBizName);
        dataMap.put("dimensions", dimensionYamlTpls);

        Map<String, Object> rootMap = new HashMap<>();
        rootMap.put("dimension", dataMap);
        return YamlUtils.toYamlWithoutNull(rootMap);

    }

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
