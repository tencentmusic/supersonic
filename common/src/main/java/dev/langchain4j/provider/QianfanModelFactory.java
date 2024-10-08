package dev.langchain4j.provider;

import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.EmbeddingModelConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.qianfan.QianfanChatModel;
import dev.langchain4j.model.qianfan.QianfanEmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
public class QianfanModelFactory implements ModelFactory, InitializingBean {

    public static final String PROVIDER = "QIANFAN";
    public static final String DEFAULT_BASE_URL = "https://aip.baidubce.com";
    public static final String DEFAULT_MODEL_NAME = "Llama-2-70b-chat";

    public static final String DEFAULT_EMBEDDING_MODEL_NAME = "Embedding-V1";
    public static final String DEFAULT_ENDPOINT = "llama_2_70b";

    @Override
    public ChatLanguageModel createChatModel(ChatModelConfig modelConfig) {
        return QianfanChatModel.builder().baseUrl(modelConfig.getBaseUrl())
                .apiKey(modelConfig.getApiKey()).secretKey(modelConfig.getSecretKey())
                .endpoint(modelConfig.getEndpoint()).modelName(modelConfig.getModelName())
                .temperature(modelConfig.getTemperature()).topP(modelConfig.getTopP())
                .maxRetries(modelConfig.getMaxRetries()).logRequests(modelConfig.getLogRequests())
                .logResponses(modelConfig.getLogResponses()).build();
    }

    @Override
    public EmbeddingModel createEmbeddingModel(EmbeddingModelConfig embeddingModelConfig) {
        return QianfanEmbeddingModel.builder().baseUrl(embeddingModelConfig.getBaseUrl())
                .apiKey(embeddingModelConfig.getApiKey())
                .secretKey(embeddingModelConfig.getSecretKey())
                .modelName(embeddingModelConfig.getModelName())
                .maxRetries(embeddingModelConfig.getMaxRetries())
                .logRequests(embeddingModelConfig.getLogRequests())
                .logResponses(embeddingModelConfig.getLogResponses()).build();
    }

    @Override
    public void afterPropertiesSet() {
        ModelProvider.add(PROVIDER, this);
    }
}
