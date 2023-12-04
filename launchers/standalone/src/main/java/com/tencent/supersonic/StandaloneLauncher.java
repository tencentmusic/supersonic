package com.tencent.supersonic;

import dev.langchain4j.S2LangChain4jAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.tencent.supersonic"},
        exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@EnableScheduling
@EnableAsync
@Import(S2LangChain4jAutoConfiguration.class)
public class StandaloneLauncher {

    public static void main(String[] args) {
        SpringApplication.run(StandaloneLauncher.class, args);
    }
}
