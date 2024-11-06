package dev.langchain4j.model.dify;

import com.tencent.supersonic.common.util.AESEncryptionUtil;
import com.tencent.supersonic.common.util.DifyClient;
import com.tencent.supersonic.common.util.DifyResult;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.Builder;

import java.util.List;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.Utils.isNullOrEmpty;
import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;
import static java.util.Collections.singletonList;

public class DifyAiChatModel implements ChatLanguageModel {

    private static final String CONTENT_TYPE_JSON = "application/json";

    private final String baseUrl;
    private final String apiKey;

    private final DifyClient difyClient;
    private final Integer maxRetries;
    private final Integer maxToken;

    private final String appName;
    private final Double temperature;
    private final Long timeOut;

    private String userName;

    @Builder
    public DifyAiChatModel(String baseUrl, String apiKey, Integer maxRetries, Integer maxToken,
            String modelName, Double temperature, Long timeOut) {
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
    }

    @Override
    public String generate(String message) {
        DifyResult difyResult = this.difyClient.generate(message, this.getUserName());
        return difyResult.getAnswer().toString();
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return generate(messages, (ToolSpecification) null);
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages,
            List<ToolSpecification> toolSpecifications) {
        ensureNotEmpty(messages, "messages");
        DifyResult difyResult =
                this.difyClient.generate(messages.get(0).text(), this.getUserName());
        System.out.println(difyResult.toString());

        if (!isNullOrEmpty(toolSpecifications)) {
            // TODO
        }

        return Response.from(AiMessage.from(difyResult.getAnswer()));
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages,
            ToolSpecification toolSpecification) {
        return generate(messages,
                toolSpecification != null ? singletonList(toolSpecification) : null);
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return null == userName ? "zhaodongsheng" : userName;
    }

}
