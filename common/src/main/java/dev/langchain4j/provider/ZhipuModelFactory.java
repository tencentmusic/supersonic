package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.zhipu.ChatCompletionModel;
import dev.langchain4j.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.zhipu.ZhipuAiEmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import static java.time.Duration.ofSeconds;

@Service
public class ZhipuModelFactory implements ModelFactory, InitializingBean {
    public static final String PROVIDER = "ZHIPU";
    public static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/";
    public static final String DEFAULT_MODEL_NAME = ChatCompletionModel.GLM_4.toString();
    public static final String DEFAULT_EMBEDDING_MODEL_NAME = "embedding-2";

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        return ZhipuAiChatModel.builder().baseUrl(modelConfig.getBaseUrl())
                .apiKey(modelConfig.getApiKey()).model(modelConfig.getModelName())
                .temperature(modelConfig.getTemperature()).topP(modelConfig.getTopP())
                .maxRetries(modelConfig.getMaxRetries()).logRequests(modelConfig.getLogRequests())
                .logResponses(modelConfig.getLogResponses()).build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        return ZhipuAiEmbeddingModel.builder().baseUrl(embeddingModelConfig.getBaseUrl())
                .apiKey(embeddingModelConfig.getApiKey()).model(embeddingModelConfig.getModelName())
                .maxRetries(embeddingModelConfig.getMaxRetries()).callTimeout(ofSeconds(60))
                .connectTimeout(ofSeconds(60)).writeTimeout(ofSeconds(60))
                .readTimeout(ofSeconds(60)).logRequests(embeddingModelConfig.getLogRequests())
                .logResponses(embeddingModelConfig.getLogResponses()).build();
    }

    @Override
    public void afterPropertiesSet() {
        ModelProvider.add(PROVIDER, this);
    }
}
