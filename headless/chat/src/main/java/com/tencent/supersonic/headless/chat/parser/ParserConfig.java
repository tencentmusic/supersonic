package com.tencent.supersonic.headless.chat.parser;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.config.ParameterConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("HeadlessParserConfig")
@Slf4j
public class ParserConfig extends ParameterConfig {

    public static final Parameter PARSER_STRATEGY_TYPE =
            new Parameter("s2.parser.s2sql.strategy", "ONE_PASS_SELF_CONSISTENCY", "LLM解析生成S2SQL策略",
                    "ONE_PASS_SELF_CONSISTENCY: 通过投票方式一步生成sql", "list", "语义解析配置",
                    Lists.newArrayList("ONE_PASS_SELF_CONSISTENCY"));

    public static final Parameter PARSER_RULE_CORRECTOR_ENABLE =
            new Parameter("s2.parser.rule.corrector.enable", "false", "是否开启规则修正器",
                    "规则修正器灵活度有限，在大模型能力足够情况下，不必强制做规则修正", "bool", "语义解析配置");

    public static final Parameter PARSER_LINKING_VALUE_ENABLE =
            new Parameter("s2.parser.linking.value.enable", "true", "是否将Mapper探测识别到的维度值提供给大模型",
                    "为了数据安全考虑, 这里可进行开关选择", "bool", "语义解析配置");

    public static final Parameter PARSER_TEXT_LENGTH_THRESHOLD =
            new Parameter("s2.parser.text.length.threshold", "10", "用户输入文本长短阈值", "文本超过该阈值为长文本",
                    "number", "语义解析配置");

    public static final Parameter PARSER_TEXT_LENGTH_THRESHOLD_SHORT =
            new Parameter("s2.parser.text.threshold.short", "0.5", "短文本匹配阈值",
                    "由于请求大模型耗时较长, 因此如果有规则类型的Query得分达到阈值,则跳过大模型的调用,"
                            + "\n如果是短文本, 若query得分/文本长度>该阈值, 则跳过当前parser",
                    "number", "语义解析配置");

    public static final Parameter PARSER_TEXT_LENGTH_THRESHOLD_LONG =
            new Parameter("s2.parser.text.threshold.long", "0.8", "长文本匹配阈值",
                    "如果是长文本, 若query得分/文本长度>该阈值, 则跳过当前parser", "number", "语义解析配置");

    public static final Parameter PARSER_EXEMPLAR_RECALL_NUMBER = new Parameter(
            "s2.parser.exemplar-recall.number", "10", "exemplar召回个数", "", "number", "语义解析配置");

    public static final Parameter PARSER_FEW_SHOT_NUMBER =
            new Parameter("s2.parser.few-shot.number", "3", "few-shot样例个数", "样例越多效果可能越好，但token消耗越大",
                    "number", "语义解析配置");

    public static final Parameter PARSER_SELF_CONSISTENCY_NUMBER =
            new Parameter("s2.parser.self-consistency.number", "1", "self-consistency执行个数",
                    "执行越多效果可能越好，但token消耗越大", "number", "语义解析配置");

    public static final Parameter PARSER_SHOW_COUNT =
            new Parameter("s2.parser.show.count", "3", "解析结果展示个数", "前端展示的解析个数", "number", "语义解析配置");

    public static final Parameter PARSER_FIELDS_COUNT_THRESHOLD =
            new Parameter("s2.parser.field.count.threshold", "0", "语义字段个数阈值",
                    "如果映射字段小于该阈值，则将数据集所有字段输入LLM", "number", "语义解析配置");

    public static final Parameter PARSER_FORMAT_JSON_TYPE =
            new Parameter("s2.parser.format.json-type", "", "请求llm返回json格式,默认不设置json格式",
                    "选项：json_schema或者json_object", "string", "语义解析配置");

    @Override
    public List<Parameter> getSysParameters() {
        return Lists.newArrayList(PARSER_LINKING_VALUE_ENABLE, PARSER_RULE_CORRECTOR_ENABLE,
                PARSER_FEW_SHOT_NUMBER, PARSER_SELF_CONSISTENCY_NUMBER, PARSER_SHOW_COUNT,
                PARSER_FIELDS_COUNT_THRESHOLD);
    }
}
