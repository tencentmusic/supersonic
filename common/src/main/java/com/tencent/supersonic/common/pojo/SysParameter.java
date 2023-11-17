package com.tencent.supersonic.common.pojo;

import com.google.common.collect.Lists;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import java.util.Arrays;
import java.util.List;

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
        Parameter parameter = new Parameter("llm.model.name", "gpt4",
                "模型名称", "list", "大语言模型相关配置");
        parameter.setCandidateValues(Lists.newArrayList("gpt3.5", "gpt3.5-16k"));
        parameters.add(parameter);
        parameters.add(new Parameter("llm.api.key", "sk-secret",
                "模型密钥", "string", "大语言模型相关配置"));
        parameters.add(new Parameter("one.detection.size", "8",
                "一次探测个数", "number", "[mapper]hanlp相关配置"));
        parameters.add(new Parameter("one.detection.max.size", "20",
                "一次探测最大个数", "number", "[mapper]hanlp相关配置"));
        parameters.add(new Parameter("metric.dimension.min.threshold", "0.3",
                "指标名、维度名最小文本相似度", "number", "[mapper]模糊匹配相关配置"));
        parameters.add(new Parameter("metric.dimension.threshold", "0.3",
                "指标名、维度名文本相似度", "number", "[mapper]模糊匹配相关配置"));
        parameters.add(new Parameter("dimension.value.threshold", "0.5",
                "维度值最小文本相似度", "number", "[mapper]模糊匹配相关配置"));
        parameters.add(new Parameter("embedding.mapper.word.min",
                "0.3", "用于向量召回最小的文本长度", "number", "[mapper]向量召回相关配置"));
        parameters.add(new Parameter("embedding.mapper.word.max", "0.3",
                "用于向量召回最大的文本长度", "number", "[mapper]向量召回相关配置"));
        parameters.add(new Parameter("embedding.mapper.batch", "0.3",
                "批量向量召回文本请求个数", "number", "[mapper]向量召回相关配置"));
        parameters.add(new Parameter("embedding.mapper.number", "0.3",
                "批量向量召回文本返回结果个数", "number", "[mapper]向量召回相关配置"));
        parameters.add(new Parameter("embedding.mapper.distance.threshold",
                "0.3", "Mapper阶段向量召回相似度阈值", "number", "[mapper]向量召回相关配置"));
        parameters.add(new Parameter("query.text.length.threshold", "0.5",
                "文本长短阈值", "number", "[parser]是否跳过当前parser相关配置"));
        parameters.add(new Parameter("short.text.threshold", "0.5",
                "短文本匹配阈值", "number", "[parser]是否跳过当前parser相关配置"));
        parameters.add(new Parameter("long.text.threshold", "0.5",
                "长文本匹配阈值", "number", "[parser]是否跳过当前parser相关配置"));
        parameters.add(new Parameter("use.s2SQL.switch", "true",
                "是否打开S2SQL转换开关", "bool", "S2SQL相关配置"));
    }

}
