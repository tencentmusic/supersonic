package com.tencent.supersonic;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.chat.api.pojo.request.QueryRequest;
import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.chat.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ConfigureDemo implements ApplicationListener<ApplicationReadyEvent>  {
    @Autowired
    private QueryService queryService;
    @Autowired
    private ChatService chatService;
    private User user = User.getFakeUser();

    public void addSampleChats()throws Exception {
        chatService.addChat(user, "样例对话1");

        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setQueryText("超音数 访问次数");
        queryRequest.setChatId(1);
        queryRequest.setUser(User.getFakeUser());
        queryService.executeQuery(queryRequest);

        queryRequest.setQueryText("按部门统计");
        queryService.executeQuery(queryRequest);

        queryRequest.setQueryText("查询近30天");
        queryService.executeQuery(queryRequest);
    }

    public void addSampleChats2() throws Exception {
        chatService.addChat(user, "样例对话2");

        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setChatId(2);
        queryRequest.setUser(User.getFakeUser());
        queryRequest.setQueryText("alice 停留时长");
        queryService.executeQuery(queryRequest);

        queryRequest.setQueryText("对比alice和lucy的访问次数");
        queryService.executeQuery(queryRequest);

        queryRequest.setQueryText("访问次数最高的部门");
        queryService.executeQuery(queryRequest);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        try {
            addSampleChats();
            addSampleChats2();
        } catch (Exception e) {
            log.error("Failed to add sample chats");
        }
    }
}
