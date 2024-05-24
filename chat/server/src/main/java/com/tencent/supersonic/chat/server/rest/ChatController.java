package com.tencent.supersonic.chat.server.rest;


import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.response.ShowCaseResp;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.server.service.ChatManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    public List<ChatDO> getAllConversions(@RequestParam(value = "agentId", required = false) Integer agentId,
                                          HttpServletRequest request, HttpServletResponse response) {
        String userName = UserHolder.findUser(request, response).getName();
        return chatService.getAll(userName, agentId);
    }

    @PostMapping("/delete")
    public Boolean deleteConversion(@RequestParam(value = "chatId") long chatId,
            HttpServletRequest request, HttpServletResponse response) {
        String userName = UserHolder.findUser(request, response).getName();
        return chatService.deleteChat(chatId, userName);
    }

    @PostMapping("/updateChatName")
    public Boolean updateConversionName(@RequestParam(value = "chatId") Long chatId,
            @RequestParam(value = "chatName") String chatName,
            HttpServletRequest request, HttpServletResponse response) {
        String userName = UserHolder.findUser(request, response).getName();
        return chatService.updateChatName(chatId, chatName, userName);
    }

    @PostMapping("/updateQAFeedback")
    public Boolean updateQAFeedback(@RequestParam(value = "id") Integer id,
            @RequestParam(value = "score") Integer score,
            @RequestParam(value = "feedback", required = false) String feedback) {
        return chatService.updateFeedback(id, score, feedback);
    }

    @PostMapping("/updateChatIsTop")
    public Boolean updateConversionIsTop(@RequestParam(value = "chatId") Long chatId,
            @RequestParam(value = "isTop") int isTop) {
        return chatService.updateChatIsTop(chatId, isTop);
    }

    @PostMapping("/pageQueryInfo")
    public PageInfo<QueryResp> pageQueryInfo(@RequestBody PageQueryInfoReq pageQueryInfoCommand,
                                             @RequestParam(value = "chatId") long chatId,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        pageQueryInfoCommand.setUserName(UserHolder.findUser(request, response).getName());
        return chatService.queryInfo(pageQueryInfoCommand, chatId);
    }

    @GetMapping("/getChatQuery/{queryId}")
    public QueryResp getChatQuery(@PathVariable("queryId") Long queryId) {
        return chatService.getChatQuery(queryId);
    }

    @PostMapping("/queryShowCase")
    public ShowCaseResp queryShowCase(@RequestBody PageQueryInfoReq pageQueryInfoCommand,
                                      @RequestParam(value = "agentId") int agentId) {
        return chatService.queryShowCase(pageQueryInfoCommand, agentId);
    }

}
