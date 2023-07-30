package com.tencent.supersonic.semantic.model.domain.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
public class YamlConfig {


    @Value("${model.yaml.file.dir: conf/models/}")
    private String metaYamlFileDir;

    public String getmetaYamlFileDir() {
        return metaYamlFileDir;
    }

}
