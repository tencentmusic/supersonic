package com.tencent.supersonic.common.config;

import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.Parameter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("AliasGenerateParameterConfig")
public class AliasGenerateParameterConfig extends ParameterConfig {
    private static final String MODULE_NAME = "智能填充配置";

    public static final Parameter LLM_ALIAS_GENERATION_MODEL =
            new Parameter("llm.alias.generation.model", "1",
                    "别名生成选用模型", "用于指标、维度等别名生成", "string", MODULE_NAME);

    public static final String ALIAS_GENERATE_TUTORIAL = 
            "#Role: You are a professional data analyst specializing in metrics and dimensions.\n"
            + "#Task: You will be provided with metadata about a metric or dimension, please help "
            + "generate a few aliases in the same language as its name.\n"
            + "#Rules:\n"
            + "1. Please do not generate aliases like xxx1, xxx2, xxx3.\n"
            + "2. Please do not generate aliases that are the same as the original names of metrics/dimensions.\n"
            + "3. Please pay attention to the quality of the generated aliases and "
            + "avoid creating aliases that look like test data.\n"
            + "4. Please output as a json string array.\n"
            + "#Output:";

    public static final Parameter LLM_ALIAS_GENERATION_PROMPT =
            new Parameter("llm.alias.generation.prompt", ALIAS_GENERATE_TUTORIAL,
                    "别名生成Prompt", "用于指标、维度等别名生成的Prompt", "longText", MODULE_NAME);

    @Override
    public List<Parameter> getSysParameters() {
        return Lists.newArrayList(LLM_ALIAS_GENERATION_MODEL, LLM_ALIAS_GENERATION_PROMPT);
    }
} 
 