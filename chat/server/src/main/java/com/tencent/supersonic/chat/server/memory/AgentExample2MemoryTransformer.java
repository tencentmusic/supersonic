package com.tencent.supersonic.chat.server.memory;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.ChatMemoryFilter;
import com.tencent.supersonic.chat.api.pojo.request.ChatParseReq;
import com.tencent.supersonic.chat.server.agent.Agent;
import com.tencent.supersonic.chat.server.persistence.dataobject.ChatMemoryDO;
import com.tencent.supersonic.chat.server.service.ChatService;
import com.tencent.supersonic.chat.server.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AgentExample2MemoryTransformer {

    @Autowired
    private ChatService chatService;

    @Autowired
    private MemoryService memoryService;

    @Async
    public void transform(Agent agent) {
        if (!agent.containsLLMParserTool() || agent.getLlmConfig() == null) {
            return;
        }
        List<String> examples = agent.getExamples();
        ChatMemoryFilter chatMemoryFilter = ChatMemoryFilter.builder().questions(examples).build();
        List<String> memoriesExisted = memoryService.getMemories(chatMemoryFilter)
                .stream().map(ChatMemoryDO::getQuestion).collect(Collectors.toList());
        for (String example : examples) {
            if (memoriesExisted.contains(example)) {
                continue;
            }
            ChatParseReq chatParseReq = new ChatParseReq();
            chatParseReq.setAgentId(agent.getId());
            chatParseReq.setQueryText(example);
            chatParseReq.setUser(User.getFakeUser());
            chatParseReq.setChatId(-1);
            chatService.parseAndExecute(chatParseReq);
        }
    }

}
