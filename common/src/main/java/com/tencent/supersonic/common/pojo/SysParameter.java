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
        parameters.add(new Parameter("llm.model.name", "gpt4",
                "模型名称(大语言模型相关配置)", "string", "大语言模型相关配置"));
        parameters.add(new Parameter("llm.api.key", "sk-afdasdasd",
                "模型密钥(大语言模型相关配置)", "string", "大语言模型相关配置"));
        parameters.add(new Parameter("one.detection.size", "8",
                "一次探测个数(hanlp相关配置)", "number", "hanlp相关配置"));
        parameters.add(new Parameter("one.detection.max.size", "20",
                "一次探测最大个数(hanlp相关配置)", "number", "hanlp相关配置"));
        parameters.add(new Parameter("metric.dimension.min.threshold", "0.3",
                "指标名、维度名最小文本相似度(mapper模糊匹配相关配置)", "number", "mapper模糊匹配相关配置"));
        parameters.add(new Parameter("metric.dimension.threshold", "0.3",
                "指标名、维度名文本相似度(mapper模糊匹配相关配置)", "number", "mapper模糊匹配相关配置"));
        parameters.add(new Parameter("dimension.value.threshold", "0.5",
                "维度值最小文本相似度(mapper模糊匹配相关配置)", "number", "mapper模糊匹配相关配置"));
        parameters.add(new Parameter("query.text.length.threshold", "0.5",
                "文本长短阈值(是否跳过当前parser相关配置)", "number", "是否跳过当前parser相关配置"));
        parameters.add(new Parameter("short.text.threshold", "0.5",
                "短文本匹配阈值(是否跳过当前parser相关配置)", "number", "是否跳过当前parser相关配置"));
        parameters.add(new Parameter("long.text.threshold", "0.5",
                "长文本匹配阈值(是否跳过当前parser相关配置)", "number", "是否跳过当前parser相关配置"));
        parameters.add(new Parameter("embedding.mapper.word.min",
                "0.3", "用于向量召回最小的文本长度(向量召回mapper相关配置)", "number", "向量召回mapper相关配置"));
        parameters.add(new Parameter("embedding.mapper.word.max", "0.3",
                "用于向量召回最大的文本长度(向量召回mapper相关配置)", "number", "向量召回mapper相关配置"));
        parameters.add(new Parameter("embedding.mapper.batch", "0.3",
                "批量向量召回文本请求个数(向量召回mapper相关配置)", "number", "向量召回mapper相关配置"));
        parameters.add(new Parameter("embedding.mapper.number", "0.3",
                "批量向量召回文本返回结果个数(向量召回mapper相关配置)", "number", "向量召回mapper相关配置"));
        parameters.add(new Parameter("embedding.mapper.distance.threshold",
                "0.3", "Mapper阶段向量召回相似度阈值(向量召回mapper相关配置)", "number", "向量召回mapper相关配置"));
        parameters.add(new Parameter("use.s2SQL.switch", "true",
                "是否打开S2SQL转换开关(S2SQL相关配置)", "bool", "S2SQL相关配置"));
    }

}
