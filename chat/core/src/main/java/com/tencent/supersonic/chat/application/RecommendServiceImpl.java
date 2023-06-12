package com.tencent.supersonic.chat.application;


import com.tencent.supersonic.chat.api.request.QueryContextReq;
import com.tencent.supersonic.chat.api.service.SemanticLayer;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.chat.domain.pojo.chat.RecommendResponse;
import com.tencent.supersonic.chat.domain.service.RecommendService;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/***
 * Recommend Service impl
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private SemanticLayer semanticLayer;

    @Override
    public RecommendResponse recommend(QueryContextReq queryCtx) {
        Integer domainId = queryCtx.getDomainId();
        if (Objects.isNull(domainId)) {
            return new RecommendResponse();
        }

        DomainSchemaResp domainSchemaDesc = semanticLayer.getDomainSchemaInfo(
                Long.valueOf(domainId));

        List<RecommendResponse.Item> dimensions = domainSchemaDesc.getDimensions().stream().map(dimSchemaDesc -> {
            RecommendResponse.Item item = new RecommendResponse.Item();
            item.setDomain(domainId);
            item.setName(dimSchemaDesc.getName());
            item.setBizName(dimSchemaDesc.getBizName());
            return item;
        }).collect(Collectors.toList());

        List<RecommendResponse.Item> metrics = domainSchemaDesc.getMetrics().stream().map(metricSchemaDesc -> {
            RecommendResponse.Item item = new RecommendResponse.Item();
            item.setDomain(domainId);
            item.setName(metricSchemaDesc.getName());
            item.setBizName(metricSchemaDesc.getBizName());
            return item;
        }).collect(Collectors.toList());

        RecommendResponse response = new RecommendResponse();
        response.setDimensions(dimensions);
        response.setMetrics(metrics);
        return response;
    }
}
