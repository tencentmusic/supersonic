package com.tencent.supersonic.chat.utils;

import static com.tencent.supersonic.common.pojo.Constants.DAY;
import static com.tencent.supersonic.common.pojo.Constants.UNDERLINE;

import com.tencent.supersonic.chat.api.component.SemanticLayer;
import com.tencent.supersonic.chat.api.pojo.DomainSchema;
import com.tencent.supersonic.chat.api.pojo.SchemaElement;
import com.tencent.supersonic.chat.api.pojo.request.KnowledgeAdvancedConfig;
import com.tencent.supersonic.chat.api.pojo.request.KnowledgeInfoReq;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigRichResp;
import com.tencent.supersonic.chat.api.pojo.response.ChatDefaultRichConfigResp;
import com.tencent.supersonic.chat.config.*;
import com.tencent.supersonic.chat.service.ConfigService;
import com.tencent.supersonic.chat.persistence.dataobject.DimValueDO;
import com.tencent.supersonic.knowledge.dictionary.DictUpdateMode;
import com.tencent.supersonic.knowledge.dictionary.DimValue2DictCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class DictMetaHelper {

    @Autowired
    private ConfigService configService;
    @Value("${model.internal.metric.suffix:internal_cnt}")
    private String internalMetricNameSuffix;
    private SemanticLayer semanticLayer = ComponentFactory.getSemanticLayer();

    public List<DimValueDO> generateDimValueInfo(DimValue2DictCommand dimValue2DictCommend) {
        List<DimValueDO> dimValueDOList = new ArrayList<>();
        DictUpdateMode updateMode = dimValue2DictCommend.getUpdateMode();
        Set<Long> domainIds = new HashSet<>();
        switch (updateMode) {
            case OFFLINE_DOMAIN:
                domainIds.addAll(dimValue2DictCommend.getDomainIds());
                dimValueDOList = generateDimValueInfoByDomain(domainIds);
                break;
            case OFFLINE_FULL:
                List<DomainSchema> domainSchemaDescList = semanticLayer.getDomainSchema();
                if (CollectionUtils.isEmpty(domainSchemaDescList)) {
                    break;
                }

                Map<Long, DomainSchema> domainIdAndDescPair = domainSchemaDescList.stream()
                        .collect(Collectors.toMap(a -> a.getDomain().getId(), schema -> schema, (k1, k2) -> k1));
                if (!CollectionUtils.isEmpty(domainIdAndDescPair)) {
                    domainIds.addAll(domainIdAndDescPair.keySet());
                    dimValueDOList = generateDimValueInfoByDomain(domainIds);
                    break;
                }
                break;
            case REALTIME_ADD:
                dimValueDOList = generateDimValueInfoByDomainAndDim(dimValue2DictCommend.getDomainAndDimPair());
                break;
            case NOT_SUPPORT:
                throw new RuntimeException("illegal parameter for updateMode");
            default:
                break;
        }

        return dimValueDOList;
    }

    private List<DimValueDO> generateDimValueInfoByDomainAndDim(Map<Long, List<Long>> domainAndDimMap) {
        List<DimValueDO> dimValueDOList = new ArrayList<>();
        if (CollectionUtils.isEmpty(domainAndDimMap)) {
            return dimValueDOList;
        }

        List<DomainSchema> domainSchemaDescList = semanticLayer.getDomainSchema();
        if (CollectionUtils.isEmpty(domainSchemaDescList)) {
            return dimValueDOList;
        }
        Map<Long, DomainSchema> domainIdAndDescPair = domainSchemaDescList.stream()
                .collect(Collectors.toMap(a -> a.getDomain().getId(), a -> a, (k1, k2) -> k1));

        for (Long domainId : domainAndDimMap.keySet()) {
            if (!domainIdAndDescPair.containsKey(domainId)) {
                continue;
            }
            Map<Long, SchemaElement> dimIdAndDescPairAll;
            dimIdAndDescPairAll = domainIdAndDescPair.get(domainId).getDimensions().stream()
                    .collect(Collectors.toMap(SchemaElement::getId, dimSchemaDesc -> dimSchemaDesc, (k1, k2) -> k1));
            List<Long> dimIdReq = domainAndDimMap.get(domainId);
            Map<Long, SchemaElement> dimIdAndDescPairReq = new HashMap<>();
            for (Long dimId : dimIdReq) {
                if (dimIdAndDescPairAll.containsKey(dimId)) {
                    dimIdAndDescPairReq.put(dimId, dimIdAndDescPairAll.get(dimId));
                }
            }
            fillDimValueDOList(dimValueDOList, domainId, dimIdAndDescPairReq);
        }

        return dimValueDOList;
    }

    private List<DimValueDO> generateDimValueInfoByDomain(Set<Long> domainIds) {
        List<DimValueDO> dimValueDOList = new ArrayList<>();
        List<DomainSchema> domainSchemaDescList = semanticLayer.getDomainSchema(new ArrayList<>(domainIds));
        if (CollectionUtils.isEmpty(domainSchemaDescList)) {
            return dimValueDOList;
        }

        domainSchemaDescList.forEach(domainSchemaDesc -> {
            Map<Long, SchemaElement> dimIdAndDescPair = domainSchemaDesc.getDimensions().stream()
                    .collect(Collectors.toMap(SchemaElement::getId, dimSchemaDesc -> dimSchemaDesc, (k1, k2) -> k1));
            fillDimValueDOList(dimValueDOList, domainSchemaDesc.getDomain().getId(), dimIdAndDescPair);

        });

        return dimValueDOList;
    }

    private void fillDimValueDOList(List<DimValueDO> dimValueDOList, Long domainId,
                                    Map<Long, SchemaElement> dimIdAndDescPair) {
        ChatConfigRichResp chaConfigRichDesc = configService.getConfigRichInfo(domainId);
        if (Objects.nonNull(chaConfigRichDesc) && Objects.nonNull(chaConfigRichDesc.getChatAggRichConfig())) {

            ChatDefaultRichConfigResp chatDefaultConfig = chaConfigRichDesc.getChatAggRichConfig().getChatDefaultConfig();
            List<KnowledgeInfoReq> knowledgeAggInfo = chaConfigRichDesc.getChatAggRichConfig().getKnowledgeInfos();

            List<KnowledgeInfoReq> knowledgeDetailInfo = chaConfigRichDesc.getChatDetailRichConfig().getKnowledgeInfos();

            fillKnowledgeDimValue(knowledgeDetailInfo, chatDefaultConfig, dimValueDOList, dimIdAndDescPair, domainId);
            fillKnowledgeDimValue(knowledgeAggInfo, chatDefaultConfig, dimValueDOList, dimIdAndDescPair, domainId);


        }
    }

    private void fillKnowledgeDimValue(List<KnowledgeInfoReq> knowledgeInfos, ChatDefaultRichConfigResp chatDefaultConfig,
                                       List<DimValueDO> dimValueDOList, Map<Long, SchemaElement> dimIdAndDescPair, Long domainId) {
        if (!CollectionUtils.isEmpty(knowledgeInfos)) {
            List<Dim4Dict> dimensions = new ArrayList<>();
            List<DefaultMetric> defaultMetricDescList = new ArrayList<>();
            knowledgeInfos.stream()
                    .filter(knowledgeInfo -> knowledgeInfo.getSearchEnable() && !CollectionUtils.isEmpty(dimIdAndDescPair)
                            && dimIdAndDescPair.containsKey(knowledgeInfo.getItemId()))
                    .forEach(knowledgeInfo -> {
                        if (dimIdAndDescPair.containsKey(knowledgeInfo.getItemId())) {
                            SchemaElement dimensionDesc = dimIdAndDescPair.get(knowledgeInfo.getItemId());

                            //default cnt
                            if (Objects.isNull(chatDefaultConfig) || CollectionUtils.isEmpty(chatDefaultConfig.getMetrics())) {
                                String datasourceBizName = dimensionDesc.getBizName();
                                if (Strings.isNotEmpty(datasourceBizName)) {
                                    String internalMetricName =
                                            datasourceBizName + UNDERLINE + internalMetricNameSuffix;
                                    defaultMetricDescList.add(new DefaultMetric(internalMetricName, 2, DAY));
                                }
                            } else {
                                SchemaElement schemaItem = chatDefaultConfig.getMetrics().get(0);
                                defaultMetricDescList.add(new DefaultMetric(schemaItem.getBizName(), chatDefaultConfig.getUnit(), chatDefaultConfig.getPeriod()));

                            }

                            String bizName = dimensionDesc.getBizName();
                            Dim4Dict dim4Dict = new Dim4Dict();
                            dim4Dict.setDimId(knowledgeInfo.getItemId());
                            dim4Dict.setBizName(bizName);
                            if(Objects.nonNull(knowledgeInfo.getKnowledgeAdvancedConfig())){
                                KnowledgeAdvancedConfig knowledgeAdvancedConfig = knowledgeInfo.getKnowledgeAdvancedConfig();
                                BeanUtils.copyProperties(knowledgeAdvancedConfig, dim4Dict);
                            }
                            dimensions.add(dim4Dict);
                        }
                    });

            if (!CollectionUtils.isEmpty(dimensions)) {
                DimValueDO dimValueDO = new DimValueDO()
                        .setDomainId(domainId)
                        .setDefaultMetricIds(defaultMetricDescList)
                        .setDimensions(dimensions);
                dimValueDOList.add(dimValueDO);
            }
        }
    }
}
