package dev.langchain4j.model.openai;

import dev.ai4j.openai4j.OpenAiClient;
import dev.ai4j.openai4j.chat.ChatCompletionChoice;
import dev.ai4j.openai4j.chat.ChatCompletionRequest;
import dev.ai4j.openai4j.chat.ChatCompletionResponse;
import dev.ai4j.openai4j.chat.ChatCompletionRequest.Builder;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.internal.RetryUtils;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.ChatModel;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.TokenCountEstimator;
import dev.langchain4j.model.output.Response;

import java.net.Proxy;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class FullOpenAiChatModel implements ChatLanguageModel, TokenCountEstimator {

    private final OpenAiClient client;
    private final String modelName;
    private final Double temperature;
    private final Double topP;
    private final List<String> stop;
    private final Integer maxTokens;
    private final Double presencePenalty;
    private final Double frequencyPenalty;
    private final Integer maxRetries;
    private final Tokenizer tokenizer;

    public FullOpenAiChatModel(String baseUrl, String apiKey, String modelName, Double temperature,
            Double topP, List<String> stop, Integer maxTokens, Double presencePenalty,
            Double frequencyPenalty, Duration timeout, Integer maxRetries, Proxy proxy,
            Boolean logRequests, Boolean logResponses, Tokenizer tokenizer) {
        baseUrl = Utils.getOrDefault(baseUrl, "https://api.openai.com/v1");
        if ("demo".equals(apiKey)) {
            baseUrl = "http://langchain4j.dev/demo/openai/v1";
        }

        timeout = Utils.getOrDefault(timeout, Duration.ofSeconds(60L));
        this.client = OpenAiClient.builder().openAiApiKey(apiKey)
                .baseUrl(baseUrl).callTimeout(timeout).connectTimeout(timeout)
                .readTimeout(timeout).writeTimeout(timeout).proxy(proxy)
                .logRequests(logRequests).logResponses(logResponses).build();
        this.modelName = Utils.getOrDefault(modelName, "gpt-3.5-turbo");
        this.temperature = Utils.getOrDefault(temperature, 0.7D);
        this.topP = topP;
        this.stop = stop;
        this.maxTokens = maxTokens;
        this.presencePenalty = presencePenalty;
        this.frequencyPenalty = frequencyPenalty;
        this.maxRetries = Utils.getOrDefault(maxRetries, 3);
        this.tokenizer = Utils.getOrDefault(tokenizer, new OpenAiTokenizer(this.modelName));
    }

    public Response<AiMessage> generate(List<ChatMessage> messages) {
        return this.generate(messages, null, null);
    }

    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        return this.generate(messages, toolSpecifications, null);
    }

    public Response<AiMessage> generate(List<ChatMessage> messages, ToolSpecification toolSpecification) {
        return this.generate(messages, Collections.singletonList(toolSpecification), toolSpecification);
    }

    private Response<AiMessage> generate(List<ChatMessage> messages,
            List<ToolSpecification> toolSpecifications,
            ToolSpecification toolThatMustBeExecuted) {
        Builder requestBuilder = null;
        if (modelName.contains(ChatModel.ZHIPU.toString()) || modelName.contains(ChatModel.ALI.toString())) {
            requestBuilder = ChatCompletionRequest.builder()
                    .model(this.modelName)
                    .messages(ImproveInternalOpenAiHelper.toOpenAiMessages(messages, this.modelName));
        } else {
            requestBuilder = ChatCompletionRequest.builder()
                    .model(this.modelName)
                    .messages(ImproveInternalOpenAiHelper.toOpenAiMessages(messages, this.modelName))
                    .temperature(this.temperature).topP(this.topP).stop(this.stop).maxTokens(this.maxTokens)
                    .presencePenalty(this.presencePenalty).frequencyPenalty(this.frequencyPenalty);
        }
        if (toolSpecifications != null && !toolSpecifications.isEmpty()) {
            requestBuilder.functions(InternalOpenAiHelper.toFunctions(toolSpecifications));
        }

        if (toolThatMustBeExecuted != null) {
            requestBuilder.functionCall(toolThatMustBeExecuted.name());
        }

        ChatCompletionRequest request = requestBuilder.build();
        ChatCompletionResponse response = (ChatCompletionResponse) RetryUtils.withRetry(() -> {
            return (ChatCompletionResponse) this.client.chatCompletion(request).execute();
        }, this.maxRetries);
        return Response.from(InternalOpenAiHelper.aiMessageFrom(response),
                InternalOpenAiHelper.tokenUsageFrom(response.usage()),
                InternalOpenAiHelper.finishReasonFrom(
                        ((ChatCompletionChoice) response.choices().get(0)).finishReason()));
    }

    public int estimateTokenCount(List<ChatMessage> messages) {
        return this.tokenizer.estimateTokenCountInMessages(messages);
    }

    public static FullOpenAiChatModel.FullOpenAiChatModelBuilder builder() {
        return new FullOpenAiChatModel.FullOpenAiChatModelBuilder();
    }

    public static class FullOpenAiChatModelBuilder {

        private String baseUrl;
        private String apiKey;
        private String modelName;
        private Double temperature;
        private Double topP;
        private List<String> stop;
        private Integer maxTokens;
        private Double presencePenalty;
        private Double frequencyPenalty;
        private Duration timeout;
        private Integer maxRetries;
        private Proxy proxy;
        private Boolean logRequests;
        private Boolean logResponses;
        private Tokenizer tokenizer;

        FullOpenAiChatModelBuilder() {
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder stop(List<String> stop) {
            this.stop = stop;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public FullOpenAiChatModel.FullOpenAiChatModelBuilder tokenizer(Tokenizer tokenizer) {
            this.tokenizer = tokenizer;
            return this;
        }

        public FullOpenAiChatModel build() {
            return new FullOpenAiChatModel(this.baseUrl, this.apiKey, this.modelName, this.temperature,
                    this.topP, this.stop, this.maxTokens, this.presencePenalty, this.frequencyPenalty,
                    this.timeout, this.maxRetries, this.proxy, this.logRequests, this.logResponses, this.tokenizer);
        }

        public String toString() {
            return "FullOpenAiChatModel.FullOpenAiChatModelBuilder(baseUrl=" + this.baseUrl
                    + ", apiKey=" + this.apiKey + ", modelName=" + this.modelName + ", temperature="
                    + this.temperature + ", topP=" + this.topP + ", stop=" + this.stop + ", maxTokens="
                    + this.maxTokens + ", presencePenalty=" + this.presencePenalty + ", frequencyPenalty="
                    + this.frequencyPenalty + ", timeout=" + this.timeout + ", maxRetries=" + this.maxRetries
                    + ", proxy=" + this.proxy + ", logRequests=" + this.logRequests + ", logResponses="
                    + this.logResponses + ", tokenizer=" + this.tokenizer + ")";
        }
    }
}
