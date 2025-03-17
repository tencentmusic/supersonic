package dev.langchain4j.model.dify;

import com.tencent.supersonic.common.util.AESEncryptionUtil;
import com.tencent.supersonic.common.util.DifyClient;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import lombok.Builder;

import java.util.List;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;

public class DifyAiStreamingChatModel implements StreamingChatLanguageModel, TokenCountEstimator {

    private final String baseUrl;
    private final String apiKey;

    private final Integer maxRetries;
    private final Integer maxToken;

    private final String appName;
    private final Double temperature;
    private final Long timeOut;

    private final DifyClient difyClient;
    private final Tokenizer tokenizer;

    private String userName;

    @Builder
    public DifyAiStreamingChatModel(String baseUrl, String apiKey, Integer maxRetries,
            Integer maxToken, String modelName, Double temperature, Long timeOut) {
        this.baseUrl = baseUrl;
        this.maxRetries = getOrDefault(maxRetries, 3);
        this.maxToken = getOrDefault(maxToken, 512);
        try {
            this.apiKey = AESEncryptionUtil.aesDecryptECB(apiKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.appName = modelName;
        this.temperature = temperature;
        this.timeOut = timeOut;

        this.difyClient = new DifyClient(this.baseUrl, this.apiKey);
        this.tokenizer = new OpenAiTokenizer();
    }

    @Override
    public int estimateTokenCount(List<ChatMessage> messages) {
        return tokenizer.estimateTokenCountInMessages(messages);
    }

    @Override
    public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
        ensureNotEmpty(messages, "messages");
        difyClient.streamingGenerate(messages.get(0).text(), this.getUserName(), handler);
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return null == userName ? "zhaodongsheng" : userName;
    }

}
