package com.tencent.supersonic.headless.core.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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

    @Value("${searchByStruct.path:/api/semantic/query/queryDimValue}")
    private String queryDimValuePath;

    @Value("${fetchModelSchemaPath.path:/api/semantic/schema}")
    private String fetchModelSchemaPath;

    @Value("${fetchModelList.path:/api/semantic/schema/dimension/page}")
    private String fetchDimensionPagePath;

    @Value("${fetchModelList.path:/api/semantic/schema/metric/page}")
    private String fetchMetricPagePath;

    @Value("${fetchModelList.path:/api/semantic/schema/domain/list}")
    private String fetchDomainListPath;

    @Value("${fetchModelList.path:/api/semantic/schema/model/list}")
    private String fetchModelListPath;

    @Value("${explain.path:/api/semantic/query/explain}")
    private String explainPath;

}
