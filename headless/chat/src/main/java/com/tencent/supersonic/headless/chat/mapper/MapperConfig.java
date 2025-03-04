package com.tencent.supersonic.headless.chat.mapper;

import com.tencent.supersonic.common.config.ParameterConfig;
import com.tencent.supersonic.common.pojo.Parameter;
import org.springframework.stereotype.Service;

@Service("HeadlessMapperConfig")
public class MapperConfig extends ParameterConfig {

    public static final Parameter MAPPER_DETECTION_SIZE = new Parameter("s2.mapper.detection.size",
            "8", "一次探测返回结果个数", "在每次探测后, 将前后缀匹配的结果合并, 并根据相似度阈值过滤后的结果个数", "number", "Mapper相关配置");

    public static final Parameter MAPPER_DETECTION_MAX_SIZE =
            new Parameter("s2.mapper.detection.max.size", "20", "一次探测前后缀匹配结果返回个数", "单次前后缀匹配返回的结果个数",
                    "number", "Mapper相关配置");

    public static final Parameter MAPPER_NAME_THRESHOLD =
            new Parameter("s2.mapper.name.threshold", "0.3", "指标名、维度名文本相似度阈值",
                    "文本片段和匹配到的指标、维度名计算出来的编辑距离阈值, 若超出该阈值, 则舍弃", "number", "Mapper相关配置");

    public static final Parameter MAPPER_NAME_THRESHOLD_MIN =
            new Parameter("s2.mapper.name.min.threshold", "0.25", "指标名、维度名最小文本相似度阈值",
                    "指标名、维度名相似度阈值在动态调整中的最低值", "number", "Mapper相关配置");

    public static final Parameter MAPPER_DIMENSION_VALUE_SIZE =
            new Parameter("s2.mapper.value.size", "1", "一次探测返回维度值结果个数",
                    "在每次探测后, 将前后缀匹配的结果合并, 并根据相似度阈值过滤后的维度值结果个数", "number", "Mapper相关配置");

    public static final Parameter MAPPER_VALUE_THRESHOLD =
            new Parameter("s2.mapper.value.threshold", "0.5", "维度值文本相似度阈值",
                    "文本片段和匹配到的维度值计算出来的编辑距离阈值, 若超出该阈值, 则舍弃", "number", "Mapper相关配置");

    public static final Parameter MAPPER_VALUE_THRESHOLD_MIN =
            new Parameter("s2.mapper.value.min.threshold", "0.3", "维度值最小文本相似度阈值",
                    "维度值相似度阈值在动态调整中的最低值", "number", "Mapper相关配置");

    public static final Parameter EMBEDDING_MAPPER_TEXT_SIZE =
            new Parameter("s2.mapper.embedding.word.size", "3", "用于向量召回文本长度",
                    "为提高向量召回效率, 按指定长度进行向量语义召回", "number", "Mapper相关配置");

    public static final Parameter EMBEDDING_MAPPER_TEXT_STEP =
            new Parameter("s2.mapper.embedding.word.step", "2", "向量召回文本每步长度",
                    "为提高向量召回效率, 按指定每步长度进行召回", "number", "Mapper相关配置");

    public static final Parameter EMBEDDING_MAPPER_BATCH =
            new Parameter("s2.mapper.embedding.batch", "50", "批量向量召回文本请求个数", "每次进行向量语义召回的原始文本片段个数",
                    "number", "Mapper相关配置");

    public static final Parameter EMBEDDING_MAPPER_NUMBER =
            new Parameter("s2.mapper.embedding.number", "5", "批量向量召回文本返回结果个数",
                    "每个文本进行向量语义召回的文本结果个数", "number", "Mapper相关配置");

    public static final Parameter EMBEDDING_MAPPER_THRESHOLD =
            new Parameter("s2.mapper.embedding.threshold", "0.9", "向量召回相似度阈值", "相似度小于该阈值的则舍弃",
                    "number", "Mapper相关配置");

    public static final Parameter EMBEDDING_MAPPER_ROUND_NUMBER =
            new Parameter("s2.mapper.embedding.round.number", "10", "向量召回最小相似度阈值",
                    "向量召回相似度阈值在动态调整中的最低值", "number", "Mapper相关配置");

    public static final Parameter EMBEDDING_MAPPER_USE_LLM =
            new Parameter("s2.mapper.embedding.use-llm-enhance", "false", "使用LLM对召回的向量进行二次判断开关",
                    "embedding的结果再通过一次LLM来筛选，这时候忽略各个向量阀值", "bool", "Mapper相关配置");

    public static final Parameter EMBEDDING_MAPPER_ALLOWED_SEGMENT_NATURE =
            new Parameter("s2.mapper.embedding.allowed-segment-nature", "['v', 'd', 'a']",
                    "使用LLM召回二次处理时对问题分词词性的控制", "分词后允许的词性才会进行向量召回", "list", "Mapper相关配置");
}
