package com.tencent.supersonic.chat.rest;


import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.domain.dataobject.ChatDO;
import com.tencent.supersonic.chat.domain.pojo.chat.ChatQueryVO;
import com.tencent.supersonic.chat.domain.pojo.chat.PageQueryInfoReq;
import com.tencent.supersonic.chat.domain.service.ChatService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/manage")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/save")
    public Boolean save(@RequestParam(value = "chatName", required = true) String chatName,
            HttpServletRequest request, HttpServletResponse response) {
        return chatService.addChat(UserHolder.findUser(request, response), chatName);
    }

    @GetMapping("/getAll")
    public List<ChatDO> getAllConversions(HttpServletRequest request, HttpServletResponse response) {
        String userName = UserHolder.findUser(request, response).getName();
        return chatService.getAll(userName);
    }

    @PostMapping("/delete")
    public Boolean deleteConversion(@RequestParam(value = "chatId", required = true) long chatId,
            HttpServletRequest request, HttpServletResponse response) {
        String userName = UserHolder.findUser(request, response).getName();
        return chatService.deleteChat(chatId, userName);
    }

    @PostMapping("/updateChatName")
    public Boolean updateConversionName(@RequestParam(value = "chatId", required = true) Long chatId,
            @RequestParam(value = "chatName", required = true) String chatName,
            HttpServletRequest request, HttpServletResponse response) {
        String userName = UserHolder.findUser(request, response).getName();
        return chatService.updateChatName(chatId, chatName, userName);
    }

    @PostMapping("/updateQAFeedback")
    public Boolean updateQAFeedback(@RequestParam(value = "id", required = true) Integer id,
            @RequestParam(value = "score", required = true) Integer score,
            @RequestParam(value = "feedback", required = true) String feedback) {
        return chatService.updateFeedback(id, score, feedback);
    }

    @PostMapping("/updateChatIsTop")
    public Boolean updateConversionIsTop(@RequestParam(value = "chatId", required = true) Long chatId,
            @RequestParam(value = "isTop", required = true) int isTop) {
        return chatService.updateChatIsTop(chatId, isTop);
    }

    @PostMapping("/pageQueryInfo")
    public PageInfo<ChatQueryVO> pageQueryInfo(@RequestBody PageQueryInfoReq pageQueryInfoCommend,
            @RequestParam(value = "chatId", required = true) long chatId,
            HttpServletRequest request,
            HttpServletResponse response) {
        pageQueryInfoCommend.setUserName(UserHolder.findUser(request, response).getName());
        return chatService.queryInfo(pageQueryInfoCommend, chatId);
    }


}
