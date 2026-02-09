package com.tencent.supersonic.headless.server.executor;

import com.tencent.supersonic.auth.api.authentication.service.UserService;
import com.tencent.supersonic.common.config.ChatModel;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.service.ChatModelService;
import dev.langchain4j.provider.ModelProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class BuiltinChatModelInitializer implements CommandLineRunner {

    private final ChatModelService chatModelService;
    private final UserService userService;
    private final Environment environment;

    @Getter
    private ChatModel demoChatModel;

    @Override
    public void run(String... args) {
        try {
            demoChatModel = initChatModelIfNotExist();
        } catch (Exception e) {
            log.error("Failed to initialize builtin chat model", e);
        }
    }

    private ChatModel initChatModelIfNotExist() {
        User user = userService.getDefaultUser();
        List<ChatModel> chatModels = chatModelService.getChatModels(user);
        if (!chatModels.isEmpty()) {
            return chatModels.getFirst();
        }
        ChatModel chatModel = new ChatModel();
        chatModel.setName("OpenAI模型DEMO");
        chatModel.setDescription("由langchain4j社区提供仅用于体验(单次请求最大token数1000), 正式使用请切换大模型");
        chatModel.setConfig(ModelProvider.DEMO_CHAT_MODEL);
        if (StringUtils.isNotBlank(environment.getProperty("OPENAI_BASE_URL"))) {
            chatModel.getConfig().setBaseUrl(environment.getProperty("OPENAI_BASE_URL"));
        }
        if (StringUtils.isNotBlank(environment.getProperty("OPENAI_API_KEY"))) {
            chatModel.getConfig().setApiKey(environment.getProperty("OPENAI_API_KEY"));
        }
        if (StringUtils.isNotBlank(environment.getProperty("OPENAI_MODEL_NAME"))) {
            chatModel.getConfig().setModelName(environment.getProperty("OPENAI_MODEL_NAME"));
        }
        log.info("Creating builtin chat model configuration");
        return chatModelService.createChatModel(chatModel, user);
    }
}
