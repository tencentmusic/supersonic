package com.tencent.supersonic.headless.server.manager;

import com.tencent.supersonic.headless.api.response.MetricResp;
import com.tencent.supersonic.headless.server.pojo.yaml.MetricYamlTpl;
import com.tencent.supersonic.headless.server.utils.MetricConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class MetricYamlManager {

    public static List<MetricYamlTpl> convert2YamlObj(List<MetricResp> metrics) {

        List<MetricYamlTpl> metricYamlTpls = new ArrayList<>();
        for (MetricResp metric : metrics) {
            MetricYamlTpl metricYamlTpl = MetricConverter.convert2MetricYamlTpl(metric);
            metricYamlTpls.add(metricYamlTpl);
        }
        return metricYamlTpls;
    }

}
