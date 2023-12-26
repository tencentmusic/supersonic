package com.tencent.supersonic.headless.model.domain.manager;

import com.tencent.supersonic.headless.common.model.yaml.MetricYamlTpl;
import com.tencent.supersonic.headless.model.domain.pojo.Metric;
import com.tencent.supersonic.headless.model.domain.utils.MetricConverter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class MetricYamlManager {

    public static List<MetricYamlTpl> convert2YamlObj(List<Metric> metrics) {

        List<MetricYamlTpl> metricYamlTpls = new ArrayList<>();
        for (Metric metric : metrics) {
            MetricYamlTpl metricYamlTpl = MetricConverter.convert2MetricYamlTpl(metric);
            metricYamlTpls.add(metricYamlTpl);
        }
        return metricYamlTpls;
    }

}
