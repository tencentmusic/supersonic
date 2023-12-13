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

        //detect config
        parameters.add(new Parameter("one.detection.size", "8",
                "hanlp一次探测返回结果个数", "在每次探测后, 将前后缀匹配的结果合并, 并根据相似度阈值过滤后的结果个数",
                "number", "Mapper相关配置"));
        parameters.add(new Parameter("one.detection.max.size", "20",
                "hanlp一次探测前后缀匹配结果返回个数", "单次前后缀匹配返回的结果个数", "number", "Mapper相关配置"));

        //mapper config
        parameters.add(new Parameter("metric.dimension.threshold", "0.3",
                "指标名、维度名文本相似度阈值", "文本片段和匹配到的指标、维度名计算出来的编辑距离阈值, 若超出该阈值, 则舍弃",
                "number", "Mapper相关配置"));
        parameters.add(new Parameter("metric.dimension.min.threshold", "0.3",
                "指标名、维度名最小文本相似度阈值",
                "最小编辑距离阈值, 在FuzzyNameMapper中, 如果上面设定的编辑距离阈值的1/2大于该最小编辑距离, 则取上面设定阈值的1/2作为阈值, 否则取该阈值",
                "number", "Mapper相关配置"));
        parameters.add(new Parameter("dimension.value.threshold", "0.5",
                "维度值最小文本相似度阈值", "文本片段和匹配到的维度值计算出来的编辑距离阈值, 若超出该阈值, 则舍弃",
                "number", "Mapper相关配置"));

        //embedding mapper config
        parameters.add(new Parameter("embedding.mapper.word.min",
                "4", "用于向量召回最小的文本长度", "为提高向量召回效率, 小于该长度的文本不进行向量语义召回", "number", "Mapper相关配置"));
        parameters.add(new Parameter("embedding.mapper.word.max", "5",
                "用于向量召回最大的文本长度", "为提高向量召回效率, 大于该长度的文本不进行向量语义召回", "number", "Mapper相关配置"));
        parameters.add(new Parameter("embedding.mapper.batch", "50",
                "批量向量召回文本请求个数", "每次进行向量语义召回的原始文本片段个数", "number", "Mapper相关配置"));
        parameters.add(new Parameter("embedding.mapper.number", "5",
                "批量向量召回文本返回结果个数", "每个文本进行向量语义召回的文本结果个数", "number", "Mapper相关配置"));
        parameters.add(new Parameter("embedding.mapper.distance.threshold",
                "0.58", "向量召回相似度阈值", "相似度大于该阈值的则舍弃", "number", "Mapper相关配置"));

        //llm config
        Parameter parameter = new Parameter("llm.model.name", "gpt3.5",
                "模型名称", "", "list", "Parser相关配置");
        parameter.setCandidateValues(Lists.newArrayList("gpt3.5", "gpt3.5-16k"));
        parameters.add(parameter);
        parameters.add(new Parameter("llm.api.key", "sk-secret",
                "模型密钥", "string", "Parser相关配置"));
        parameters.add(new Parameter("llm.temperature", "0.0",
                "温度值", "number", "Parser相关配置"));

        Parameter s2SQLParameter = new Parameter("s2SQL.generation", "2_pass_auto_cot_self_consistency",
                "S2SQL生成方式", "list", "Parser相关配置");
        s2SQLParameter.setCandidateValues(Lists.newArrayList("1_pass_auto_cot", "1_pass_auto_cot_self_consistency",
                "2_pass_auto_cot", "2_pass_auto_cot_self_consistency"));
        parameters.add(s2SQLParameter);
        parameters.add(new Parameter("s2SQL.linking.value.switch", "true",
                "是否将Mapper探测识别到的维度值提供给大模型", "为了数据安全考虑, 这里可进行开关选择",
                "bool", "Parser相关配置"));

        parameters.add(new Parameter("query.text.length.threshold", "10",
                "用户输入文本长短阈值", "文本超过该阈值为长文本", "number", "Parser相关配置"));
        parameters.add(new Parameter("short.text.threshold", "0.5",
                "短文本匹配阈值", "由于请求大模型耗时较长, 因此如果有规则类型的Query得分达到阈值,则跳过大模型的调用,\n如果是短文本, 若query得分/文本长度>该阈值, 则跳过当前parser",
                "number", "Parser相关配置"));
        parameters.add(new Parameter("long.text.threshold", "0.8",
                "长文本匹配阈值", "如果是长文本, 若query得分/文本长度>该阈值, 则跳过当前parser",
                "number", "Parser相关配置"));
    }

}
