package com.tencent.supersonic.chat.test;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication(scanBasePackages = {"com.tencent.supersonic"}
        //  , exclude = {DataSourceAutoConfiguration.class}
)
@ComponentScan("com.tencent.supersonic")
@MapperScan("com.tencent.supersonic")
public class ChatBizLauncher {

    public static void main(String[] args) {
        SpringApplication.run(ChatBizLauncher.class, args);
    }

}
