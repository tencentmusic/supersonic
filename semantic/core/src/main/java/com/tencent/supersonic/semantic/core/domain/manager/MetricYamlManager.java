package com.tencent.supersonic.semantic.core.domain.manager;

import com.tencent.supersonic.semantic.api.core.pojo.yaml.MetricYamlTpl;
import com.tencent.supersonic.common.enums.TypeEnums;
import com.tencent.supersonic.common.util.yaml.YamlUtils;
import com.tencent.supersonic.semantic.core.domain.pojo.Metric;
import com.tencent.supersonic.semantic.core.domain.utils.MetricConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Slf4j
@Service
public class MetricYamlManager {


    private YamlManager yamlManager;

    public MetricYamlManager(YamlManager yamlManager) {
        this.yamlManager = yamlManager;
    }


    public void generateYamlFile(List<Metric> metrics, String fullPath, String domainBizName) throws Exception {
        String yamlStr = convert2YamlStr(metrics);
        log.info("generate yaml str :{} from metric:{} full path:{}", yamlStr, metrics, fullPath);
        yamlManager.generateYamlFile(yamlStr, fullPath, getYamlName(domainBizName));
    }

    public String getYamlName(String name) {
        return String.format("%s_%s", name, TypeEnums.METRIC.getName());
    }

    public static String convert2YamlStr(List<Metric> metrics) {
        if (CollectionUtils.isEmpty(metrics)) {
            return "";
        }
        StringBuilder yamlBuilder = new StringBuilder();
        for (Metric metric : metrics) {
            MetricYamlTpl metricYamlTpl = MetricConverter.convert2MetricYamlTpl(metric);
            Map<String, Object> rootMap = new HashMap<>();
            rootMap.put("metric", metricYamlTpl);
            yamlBuilder.append(YamlUtils.toYamlWithoutNull(rootMap)).append("\n");
        }
        return yamlBuilder.toString();
    }

    public static List<MetricYamlTpl> convert2YamlObj(List<Metric> metrics) {

        List<MetricYamlTpl> metricYamlTpls = new ArrayList<>();
        for (Metric metric : metrics) {
            MetricYamlTpl metricYamlTpl = MetricConverter.convert2MetricYamlTpl(metric);
            metricYamlTpls.add(metricYamlTpl);
        }
        return metricYamlTpls;
    }

}
