package com.tencent.supersonic.knowledge.semantic;

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

    @Value("${searchByStruct.path:/api/semantic/query/multiStruct}")
    private String searchByMultiStructPath;

    @Value("${searchByStruct.path:/api/semantic/query/sql}")
    private String searchBySqlPath;

    @Value("${fetchDomainSchemaPath.path:/api/semantic/schema}")
    private String fetchDomainSchemaPath;

    @Value("${fetchDomainList.path:/api/semantic/schema/dimension/page}")
    private String fetchDimensionPagePath;

    @Value("${fetchDomainList.path:/api/semantic/schema/metric/page}")
    private String fetchMetricPagePath;

    @Value("${fetchDomainList.path:/api/semantic/schema/domain/list}")
    private String fetchDomainListPath;

    @Value("${fetchDomainList.path:/api/semantic/schema/domain/view/list}")
    private String fetchDomainViewListPath;

}
