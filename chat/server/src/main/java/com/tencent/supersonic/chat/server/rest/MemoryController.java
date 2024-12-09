package com.tencent.supersonic.chat.server.rest;

import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.chat.api.pojo.enums.MemoryReviewResult;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryCreateReq;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryUpdateReq;
import com.tencent.supersonic.chat.api.pojo.request.PageMemoryReq;
import com.tencent.supersonic.chat.server.pojo.ChatMemory;
import com.tencent.supersonic.chat.server.service.MemoryService;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping({"/api/chat/memory"})
public class MemoryController {

    @Autowired
    private MemoryService memoryService;

    @PostMapping("/createMemory")
    public Boolean createMemory(@RequestBody ChatMemoryCreateReq chatMemoryCreateReq,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        memoryService.createMemory(ChatMemory.builder().agentId(chatMemoryCreateReq.getAgentId())
                .s2sql(chatMemoryCreateReq.getS2sql()).question(chatMemoryCreateReq.getQuestion())
                .dbSchema(chatMemoryCreateReq.getDbSchema()).status(chatMemoryCreateReq.getStatus())
                .humanReviewRet(MemoryReviewResult.POSITIVE).createdBy(user.getName())
                .createdAt(new Date()).build());
        return true;
    }

    @PostMapping("/updateMemory")
    public Boolean updateMemory(@RequestBody ChatMemoryUpdateReq chatMemoryUpdateReq,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        memoryService.updateMemory(chatMemoryUpdateReq, user);
        return true;
    }

    @RequestMapping("/pageMemories")
    public PageInfo<ChatMemory> pageMemories(@RequestBody PageMemoryReq pageMemoryReq) {
        return memoryService.pageMemories(pageMemoryReq);
    }

    @PostMapping("batchDelete")
    public Boolean batchDelete(@RequestBody MetaBatchReq metaBatchReq) {
        memoryService.batchDelete(metaBatchReq.getIds());
        return true;
    }
}
