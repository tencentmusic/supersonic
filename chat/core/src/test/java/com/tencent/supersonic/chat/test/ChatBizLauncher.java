package com.tencent.supersonic.chat.test;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication(scanBasePackages = {"com.tencent.supersonic.chat"})
@ComponentScan("com.tencent.supersonic.chat")
@MapperScan("com.tencent.supersonic.chat")
public class ChatBizLauncher {

    public static void main(String[] args) {
        SpringApplication.run(ChatBizLauncher.class, args);
    }

}
