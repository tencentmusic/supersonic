package dev.langchain4j.model.openai;

import dev.ai4j.openai4j.chat.ChatCompletionChoice;
import dev.ai4j.openai4j.chat.ChatCompletionResponse;
import dev.ai4j.openai4j.chat.Function;
import dev.ai4j.openai4j.chat.FunctionCall;
import dev.ai4j.openai4j.chat.Message;
import dev.ai4j.openai4j.chat.Parameters;
import dev.ai4j.openai4j.chat.Role;
import dev.ai4j.openai4j.shared.Usage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ChatModel;
import dev.langchain4j.model.output.TokenUsage;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ImproveInternalOpenAiHelper {
    static final String OPENAI_URL = "https://api.openai.com/v1";
    static final String OPENAI_DEMO_API_KEY = "demo";
    static final String OPENAI_DEMO_URL = "http://langchain4j.dev/demo/openai/v1";

    public ImproveInternalOpenAiHelper() {
    }

    public static List<Message> toOpenAiMessages(List<ChatMessage> messages, String modelName) {
        List<Message> messageList = messages.stream()
                .map(message -> toOpenAiMessage(message, modelName)).collect(Collectors.toList());
        return messageList;
    }

    public static Message toOpenAiMessage(ChatMessage message, String modelName) {
        return Message.builder().role(roleFrom(message, modelName))
                .name(nameFrom(message)).content(message.text())
                .functionCall(functionCallFrom(message)).build();
    }

    private static String nameFrom(ChatMessage message) {
        if (message instanceof UserMessage) {
            return ((UserMessage) message).name();
        } else {
            return message instanceof ToolExecutionResultMessage
                    ? ((ToolExecutionResultMessage) message).toolName() : null;
        }
    }

    private static FunctionCall functionCallFrom(ChatMessage message) {
        if (message instanceof AiMessage) {
            AiMessage aiMessage = (AiMessage) message;
            if (aiMessage.toolExecutionRequest() != null) {
                return FunctionCall.builder().name(aiMessage.toolExecutionRequest().name())
                        .arguments(aiMessage.toolExecutionRequest().arguments()).build();
            }
        }

        return null;
    }

    public static Role roleFrom(ChatMessage message, String modelName) {
        if (modelName.contains(ChatModel.ZHIPU.toString()) || modelName.contains(ChatModel.ALI.toString())) {
            return Role.USER;
        }
        if (message instanceof AiMessage) {
            return Role.ASSISTANT;
        } else if (message instanceof ToolExecutionResultMessage) {
            return Role.FUNCTION;
        } else {
            return message instanceof SystemMessage ? Role.SYSTEM : Role.USER;
        }
    }

    public static List<Function> toFunctions(Collection<ToolSpecification> toolSpecifications) {
        return (List) toolSpecifications.stream().map(ImproveInternalOpenAiHelper::toFunction)
                .collect(Collectors.toList());
    }

    private static Function toFunction(ToolSpecification toolSpecification) {
        return Function.builder().name(toolSpecification.name())
                .description(toolSpecification.description())
                .parameters(toOpenAiParameters(toolSpecification.parameters())).build();
    }

    private static Parameters toOpenAiParameters(ToolParameters toolParameters) {
        return toolParameters == null ? Parameters.builder().build() : Parameters.builder()
                .properties(toolParameters.properties()).required(toolParameters.required()).build();
    }

    public static AiMessage aiMessageFrom(ChatCompletionResponse response) {
        if (response.content() != null) {
            return AiMessage.aiMessage(response.content());
        } else {
            FunctionCall functionCall = ((ChatCompletionChoice) response.choices().get(0)).message().functionCall();
            ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                    .name(functionCall.name()).arguments(functionCall.arguments()).build();
            return AiMessage.aiMessage(toolExecutionRequest);
        }
    }

    public static TokenUsage tokenUsageFrom(Usage openAiUsage) {
        return openAiUsage == null ? null : new TokenUsage(openAiUsage.promptTokens(),
                openAiUsage.completionTokens(), openAiUsage.totalTokens());
    }
}
