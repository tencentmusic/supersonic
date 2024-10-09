package com.tencent.supersonic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Chat Launcher */
@SpringBootApplication(scanBasePackages = {"com.tencent.supersonic"},
        exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@EnableScheduling
public class ChatLauncher {

    public static void main(String[] args) {
        SpringApplication.run(ChatLauncher.class, args);
    }
}
