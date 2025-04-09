package com.tencent.supersonic.chat.server.rest;

import com.tencent.supersonic.chat.api.pojo.request.ChatExecuteReq;
import com.tencent.supersonic.chat.server.service.DeepSeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat/crab")
public class crabController {
    @Autowired
    private DeepSeekService deepSeekService;

    @PostMapping(value = "/deepSeekStream")
    public SseEmitter streamChat(@RequestBody ChatExecuteReq chatExecuteReq) {
        return deepSeekService.streamChat(chatExecuteReq);
    }
}
