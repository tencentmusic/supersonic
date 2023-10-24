package com.tencent.supersonic.chat.rest;


import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.response.QueryRecallResp;
import com.tencent.supersonic.chat.api.pojo.response.ShowCaseResp;
import com.tencent.supersonic.chat.api.pojo.response.SolvedQueryRecallResp;
import com.tencent.supersonic.chat.persistence.dataobject.ChatDO;
import com.tencent.supersonic.chat.api.pojo.response.QueryResp;
import com.tencent.supersonic.chat.api.pojo.request.PageQueryInfoReq;
import com.tencent.supersonic.chat.service.ChatService;
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
@RequestMapping({"/api/chat/manage", "/openapi/chat/manage"})
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/save")
    public Boolean save(@RequestParam(value = "chatName") String chatName,
                        @RequestParam(value = "agentId", required = false) Integer agentId,
            HttpServletRequest request, HttpServletResponse response) {
        return chatService.addChat(UserHolder.findUser(request, response), chatName, agentId);
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

    @PostMapping("/queryShowCase")
    public ShowCaseResp queryShowCase(@RequestBody PageQueryInfoReq pageQueryInfoCommand,
                                      @RequestParam(value = "agentId") int agentId) {
        return chatService.queryShowCase(pageQueryInfoCommand, agentId);
    }

    @RequestMapping("/getSolvedQuery")
    public List<SolvedQueryRecallResp> getSolvedQuery(@RequestParam(value = "queryText") String queryText,
                                                      @RequestParam(value = "agentId") Integer agentId) {
        QueryRecallResp queryRecallResp = new QueryRecallResp();
        Long startTime = System.currentTimeMillis();
        List<SolvedQueryRecallResp> solvedQueryRecallRespList = chatService.getSolvedQuery(queryText, agentId);
        queryRecallResp.setSolvedQueryRecallRespList(solvedQueryRecallRespList);
        queryRecallResp.setQueryTimeCost(System.currentTimeMillis() - startTime);
        return solvedQueryRecallRespList;
    }

}
