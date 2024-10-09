package com.tencent.supersonic.chat.server.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.response.ChatModelTypeResp;
import com.tencent.supersonic.chat.server.pojo.ChatModel;
import com.tencent.supersonic.chat.server.service.ChatModelService;
import com.tencent.supersonic.chat.server.util.ModelConfigHelper;
import com.tencent.supersonic.common.pojo.ChatModelConfig;
import com.tencent.supersonic.common.pojo.enums.ChatModelType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/chat/model", "/openapi/chat/model"})
public class ChatModelController {
    @Autowired
    private ChatModelService chatModelService;

    @PostMapping
    public ChatModel createModel(@RequestBody ChatModel model,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        return chatModelService.createChatModel(model, user);
    }

    @PutMapping
    public ChatModel updateModel(@RequestBody ChatModel model,
            HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        User user = UserHolder.findUser(httpServletRequest, httpServletResponse);
        return chatModelService.updateChatModel(model, user);
    }

    @DeleteMapping("/{id}")
    public boolean deleteModel(@PathVariable("id") Integer id) {
        chatModelService.deleteChatModel(id);
        return true;
    }

    @RequestMapping("/getModelList")
    public List<ChatModel> getModelList() {
        return chatModelService.getChatModels();
    }

    @RequestMapping("/getModelTypeList")
    public List<ChatModelTypeResp> getModelTypeList() {
        return Arrays.stream(ChatModelType.values()).map(t -> ChatModelTypeResp.builder()
                .type(t.toString()).name(t.getName()).description(t.getDescription()).build())
                .collect(Collectors.toList());
    }

    @PostMapping("/testConnection")
    public boolean testConnection(@RequestBody ChatModelConfig modelConfig) {
        return ModelConfigHelper.testConnection(modelConfig);
    }
}
