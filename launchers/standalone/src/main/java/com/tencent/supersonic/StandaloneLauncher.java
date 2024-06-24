package com.tencent.supersonic;

import com.cvte.psd.conf.core.spring.annotation.EnableApolloConfig;
import dev.langchain4j.S2LangChain4jAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(scanBasePackages = {"com.tencent.supersonic"},
        exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class},
        excludeName = {"spring.dev.langchain4j.spring.LangChain4jAutoConfig",
                "spring.dev.langchain4j.openai.spring.AutoConfig",
                "spring.dev.langchain4j.ollama.spring.AutoConfig",
                "spring.dev.langchain4j.azure.openai.spring.AutoConfig",
                "spring.dev.langchain4j.azure.aisearch.spring.AutoConfig",
                "spring.dev.langchain4j.anthropic.spring.AutoConfig"
        })
@EnableScheduling
@EnableAsync
@Configuration
@EnableApolloConfig
@Import(S2LangChain4jAutoConfiguration.class)
@EnableSwagger2
public class StandaloneLauncher {

    public static void main(String[] args) {
        SpringApplication.run(StandaloneLauncher.class, args);
    }
}
