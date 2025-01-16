package com.tencent.supersonic.chat.server.rest;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.ShowCaseResp;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/chat/manage", "/openapi/chat/manage"})
public class ChatController {

    @Autowired
    private ChatManageService chatService;

    @PostMapping("/save")
    public Boolean save(@RequestParam(value = "chatName") String chatName,
            @RequestParam(value = "agentId", required = false) Integer agentId,
            HttpServletRequest request, HttpServletResponse response) {
        chatService.addChat(UserHolder.findUser(request, response), chatName, agentId);
        return true;
    }

    @GetMapping("/getAll")
    public List<ChatDO> getAllChats(
            @RequestParam(value = "agentId", required = false) Integer agentId,
            HttpServletRequest request, HttpServletResponse response) {
        String userName = UserHolder.findUser(request, response).getName();
        return chatService.getAll(userName, agentId);
    }

    @PostMapping("/delete")
    public Boolean deleteChat(@RequestParam(value = "chatId") long chatId,
            HttpServletRequest request, HttpServletResponse response) {
        String userName = UserHolder.findUser(request, response).getName();
        return chatService.deleteChat(chatId, userName);
    }

    @PostMapping("/updateChatName")
    public Boolean updateChatName(@RequestParam(value = "chatId") Long chatId,
            @RequestParam(value = "chatName") String chatName, HttpServletRequest request,
            HttpServletResponse response) {
        String userName = UserHolder.findUser(request, response).getName();
        return chatService.updateChatName(chatId, chatName, userName);
    }

    @PostMapping("/updateQAFeedback")
    public Boolean updateQAFeedback(@RequestParam(value = "id") Long id,
            @RequestParam(value = "score") Integer score,
            @RequestParam(value = "feedback", required = false) String feedback) {
        return chatService.updateFeedback(id, score, feedback);
    }

    @PostMapping("/updateChatIsTop")
    public Boolean updateChatIsTop(@RequestParam(value = "chatId") Long chatId,
            @RequestParam(value = "isTop") int isTop) {
        return chatService.updateChatIsTop(chatId, isTop);
    }

    @PostMapping("/pageQueryInfo")
    public PageInfo<QueryResp> pageQueryInfo(@RequestBody PageQueryInfoReq pageQueryInfoCommand,
            @RequestParam(value = "chatId") long chatId, HttpServletRequest request,
            HttpServletResponse response) {
        pageQueryInfoCommand.setUserName(UserHolder.findUser(request, response).getName());
        return chatService.queryInfo(pageQueryInfoCommand, chatId);
    }

    @GetMapping("/getChatQuery/{queryId}")
    public QueryResp getChatQuery(@PathVariable("queryId") Long queryId) {
        return chatService.getChatQuery(queryId);
    }

    @DeleteMapping("/{queryId}")
    public boolean deleteChatQuery(@PathVariable(value = "queryId") Long queryId) {
        chatService.deleteQuery(queryId);
        return true;
    }

    @PostMapping("/queryShowCase")
    public ShowCaseResp queryShowCase(@RequestBody PageQueryInfoReq pageQueryInfoCommand,
            @RequestParam(value = "agentId") int agentId) {
        return chatService.queryShowCase(pageQueryInfoCommand, agentId);
    }
}
