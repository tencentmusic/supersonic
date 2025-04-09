package com.tencent.supersonic.headless.chat.mapper;

import com.alibaba.fastjson.JSON;
import com.tencent.supersonic.common.pojo.ChatApp;
import com.tencent.supersonic.headless.api.pojo.request.QueryNLReq;
import com.tencent.supersonic.headless.api.pojo.response.S2Term;
import com.tencent.supersonic.headless.chat.ChatQueryContext;
import com.tencent.supersonic.headless.chat.knowledge.MapResult;
import com.tencent.supersonic.headless.chat.parser.llm.OnePassSCSqlGenStrategy;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.provider.ModelProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.tencent.supersonic.headless.chat.mapper.MapperConfig.EMBEDDING_MAPPER_USE_LLM;
import static com.tencent.supersonic.headless.chat.mapper.MapperConfig.EMBEDDING_MATCH_USE_LLM_WORDS_SEGMENT;

@Service
@Slf4j
public abstract class BatchMatchStrategy<T extends MapResult> extends BaseMatchStrategy<T> {

    public static final String LLM_WORDS_SEGMENT_PROMPT = "任务描述：你的任务是接收用户关于数据指标查询的问题输入，并将其按照中文语法规则准确地分割成独立的词汇单元" +
            "提取其中的维度/指标/维度值" +
            "每个词汇或短语应当能够作为搜索数据中台内对应指标名称/维度名词/维度值。" +
            "输入示例：国色芳华最近一周的播放次数是多少？" +
            "输出格式应为json数据格式，只输出json数组，不要输出其他内容，输出格式示例：[\"国色芳华\",\"播放次数\"]\n" +
            "输入问题为:{{text}}";

    @Autowired
    protected MapperConfig mapperConfig;

    @Override
    public List<T> detect(ChatQueryContext chatQueryContext, List<S2Term> terms,
                          Set<Long> detectDataSetIds) {

        String text = chatQueryContext.getRequest().getQueryText();
        Set<String> detectSegments = new HashSet<>();
        boolean useLLMWordsSegment =
                Boolean.parseBoolean(mapperConfig.getParameterValue(EMBEDDING_MATCH_USE_LLM_WORDS_SEGMENT));


        if (useLLMWordsSegment) {
            useLLMSplit(detectSegments, text, chatQueryContext.getRequest());
        } else {
            int embeddingTextSize = Integer
                    .valueOf(mapperConfig.getParameterValue(MapperConfig.EMBEDDING_MAPPER_TEXT_SIZE));

            int embeddingTextStep = Integer
                    .valueOf(mapperConfig.getParameterValue(MapperConfig.EMBEDDING_MAPPER_TEXT_STEP));

            for (int startIndex = 0; startIndex < text.length(); startIndex += embeddingTextStep) {
                int endIndex = Math.min(startIndex + embeddingTextSize, text.length());
                String detectSegment = text.substring(startIndex, endIndex).trim();
                detectSegments.add(detectSegment);
            }
        }
        return detectByBatch(chatQueryContext, detectDataSetIds, detectSegments);
    }

    //    通过llm进行分词
    private void useLLMSplit(Set<String> detectSegments, String text, QueryNLReq request) {
        Map<String, Object> variable = new HashMap<>();
        variable.put("text", text);

        ChatApp chatApp = request.getChatAppConfig().get(OnePassSCSqlGenStrategy.APP_KEY);
        Prompt prompt = PromptTemplate.from(LLM_WORDS_SEGMENT_PROMPT).apply(variable);
        ChatLanguageModel chatLanguageModel = ModelProvider.getChatModel(chatApp.getChatModelConfig());
        String response = chatLanguageModel.generate(prompt.toUserMessage().singleText());
        if (StringUtils.isNotBlank(response)) {
            List<String> words = JSON.parseArray(response, String.class);
            detectSegments.addAll(words);

        }
    }

    public abstract List<T> detectByBatch(ChatQueryContext chatQueryContext,
                                          Set<Long> detectDataSetIds, Set<String> detectSegments);
}
