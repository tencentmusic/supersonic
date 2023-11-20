package com.tencent.supersonic.common.pojo;

import com.google.common.collect.Lists;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class SysParameter {

    private Integer id;

    private List<String> admins;

    private List<Parameter> parameters;

    public String getAdmin() {
        if (CollectionUtils.isEmpty(admins)) {
            return "";
        }
        return StringUtils.join(admins, ",");
    }

    public String getParameterByName(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }
        Map<String, String> nameToValue = parameters.stream()
                .collect(Collectors.toMap(Parameter::getName, Parameter::getValue, (k1, k2) -> k1));
        return nameToValue.get(name);
    }

    public void setAdminList(String admin) {
        if (StringUtils.isNotBlank(admin)) {
            admins = Arrays.asList(admin.split(","));
        } else {
            admins = Lists.newArrayList();
        }
    }

    public void init() {
        parameters = Lists.newArrayList();
        admins = Lists.newArrayList("admin");
        //llm config
        Parameter parameter = new Parameter("llm.model.name", "gpt3.5",
                "模型名称", "list", "大语言模型相关配置");
        parameter.setCandidateValues(Lists.newArrayList("gpt3.5", "gpt3.5-16k"));
        parameters.add(parameter);
        parameters.add(new Parameter("llm.api.key", "sk-secret",
                "模型密钥", "string", "大语言模型相关配置"));
        parameters.add(new Parameter("llm.temperature", "0.0",
                "温度值", "number", "大语言模型相关配置"));

        //s2SQL config
        parameters.add(new Parameter("s2SQL.generation", "2-steps",
                "S2SQL生成方式", "string", "S2SQL相关配置"));
        parameters.add(new Parameter("s2SQL.linking.value.switch", "true",
                "是否将linkingValues提供给大模型", "bool", "S2SQL相关配置"));

        //detect config
        parameters.add(new Parameter("one.detection.size", "8",
                "一次探测个数", "number", "[mapper]hanlp相关配置"));
        parameters.add(new Parameter("one.detection.max.size", "20",
                "一次探测最大个数", "number", "[mapper]hanlp相关配置"));

        //mapper config
        parameters.add(new Parameter("metric.dimension.min.threshold", "0.3",
                "指标名、维度名最小文本相似度", "number", "[mapper]模糊匹配相关配置"));
        parameters.add(new Parameter("metric.dimension.threshold", "0.3",
                "指标名、维度名文本相似度", "number", "[mapper]模糊匹配相关配置"));
        parameters.add(new Parameter("dimension.value.threshold", "0.5",
                "维度值最小文本相似度", "number", "[mapper]模糊匹配相关配置"));

        //embedding mapper config
        parameters.add(new Parameter("embedding.mapper.word.min",
                "4", "用于向量召回最小的文本长度", "number", "[mapper]向量召回相关配置"));
        parameters.add(new Parameter("embedding.mapper.word.max", "5",
                "用于向量召回最大的文本长度", "number", "[mapper]向量召回相关配置"));
        parameters.add(new Parameter("embedding.mapper.batch", "50",
                "批量向量召回文本请求个数", "number", "[mapper]向量召回相关配置"));
        parameters.add(new Parameter("embedding.mapper.number", "5",
                "批量向量召回文本返回结果个数", "number", "[mapper]向量召回相关配置"));
        parameters.add(new Parameter("embedding.mapper.distance.threshold",
                "0.58", "向量召回相似度阈值", "number", "[mapper]向量召回相关配置"));

        //skip config
        parameters.add(new Parameter("query.text.length.threshold", "10",
                "文本长短阈值", "文本超过该阈值为长文本", "number", "是否跳过当前parser相关配置"));
        parameters.add(new Parameter("short.text.threshold", "0.5",
                "短文本匹配阈值", "如果是短文本, 若query得分/文本长度>该阈值, 则跳过当前parser",
                "number", "是否跳过当前parser相关配置"));
        parameters.add(new Parameter("long.text.threshold", "0.8",
                "长文本匹配阈值", "如果是长文本, 若query得分/文本长度>该阈值, 则跳过当前parser",
                "number", "是否跳过当前parser相关配置"));
    }

}
