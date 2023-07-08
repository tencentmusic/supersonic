package com.tencent.supersonic.chat.infrastructure.semantic;

import com.tencent.supersonic.chat.application.ConfigServiceImpl;
import com.tencent.supersonic.chat.domain.utils.DefaultSemanticInternalUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Data
public class DefaultSemanticConfig {

    @Value("${semantic.url.prefix:http://localhost:8081}")
    private String semanticUrl;

    @Value("${searchByStruct.path:/api/semantic/query/struct}")
    private String searchByStructPath;

    @Value("${searchByStruct.path:/api/semantic/query/sql}")
    private String searchBySqlPath;

    @Value("${fetchDomainSchemaPath.path:/api/semantic/schema}")
    private String fetchDomainSchemaPath;

    @Autowired
    private DefaultSemanticInternalUtils defaultSemanticInternalUtils;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConfigServiceImpl chaConfigService;
}
