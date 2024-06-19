package dev.langchain4j.model.openai;

import dev.ai4j.openai4j.chat.FunctionCall;
import dev.ai4j.openai4j.chat.Message;
import dev.ai4j.openai4j.chat.Role;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.ChatModel;
import java.util.List;
import java.util.stream.Collectors;

public class ImproveInternalOpenAiHelper {

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

}
