package com.tencent.supersonic.chat.test.context;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.service.SemanticLayer;
import com.tencent.supersonic.semantic.api.core.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.semantic.api.core.response.MetricSchemaResp;
import com.tencent.supersonic.chat.application.ConfigServiceImpl;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigInfo;
import com.tencent.supersonic.chat.domain.pojo.config.ChatConfigRichInfo;
import com.tencent.supersonic.chat.domain.pojo.config.DefaultMetric;
import com.tencent.supersonic.chat.domain.pojo.config.DefaultMetricInfo;
import com.tencent.supersonic.chat.domain.pojo.config.EntityInternalDetail;
import com.tencent.supersonic.chat.domain.pojo.config.EntityRichInfo;
import com.tencent.supersonic.chat.domain.pojo.semantic.DomainInfos;
import com.tencent.supersonic.chat.domain.service.ChatService;
import com.tencent.supersonic.chat.domain.service.QueryService;
import com.tencent.supersonic.chat.domain.utils.SchemaInfoConverter;
import com.tencent.supersonic.chat.infrastructure.mapper.ChatContextMapper;
import com.tencent.supersonic.chat.infrastructure.repository.ChatContextRepositoryImpl;
import com.tencent.supersonic.common.constant.Constants;
import com.tencent.supersonic.semantic.core.domain.DimensionService;
import com.tencent.supersonic.semantic.core.domain.DomainService;
import com.tencent.supersonic.semantic.core.domain.MetricService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MockBeansConfiguration {

    @Bean
    public ChatContextRepositoryImpl getChatContextRepository() {
        return Mockito.mock(ChatContextRepositoryImpl.class);
    }

    @Bean
    public QueryService getQueryService() {
        return Mockito.mock(QueryService.class);
    }

    @Bean
    public DimensionService getDimensionService() {
        return Mockito.mock(DimensionService.class);
    }

    @Bean
    public MetricService getMetricService() {
        return Mockito.mock(MetricService.class);
    }

    @Bean
    public DomainService getDomainService() {
        return Mockito.mock(DomainService.class);
    }

    @Bean
    public ChatContextMapper getChatContextMapper() {
        return Mockito.mock(ChatContextMapper.class);
    }

    @Bean
    public ConfigServiceImpl getDomainExtendService() {
        return Mockito.mock(ConfigServiceImpl.class);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
//    @Bean
//    public SemanticLayer getSemanticService() {
//        return Mockito.mock(HttpSemanticServiceImpl.class);
//    }


    public static void getOrCreateContextMock(ChatService chatService) {
        ChatContext context = new ChatContext();
        context.setChatId(1);
        when(chatService.getOrCreateContext(1)).thenReturn(context);
    }

    public static void buildHttpSemanticServiceImpl(SemanticLayer httpSemanticLayer, List<DimSchemaResp> dimensionDescs,
            List<MetricSchemaResp> metricDescs) {
        ChatConfigRichInfo chaConfigRichDesc = new ChatConfigRichInfo();
        DefaultMetric defaultMetricDesc = new DefaultMetric();
        defaultMetricDesc.setUnit(3);
        defaultMetricDesc.setPeriod(Constants.DAY);
        chaConfigRichDesc.setDefaultMetrics(new ArrayList<>(Arrays.asList(defaultMetricDesc)));
        EntityRichInfo entityDesc = new EntityRichInfo();
        List<DimSchemaResp> dimensionDescs1 = new ArrayList<>();
        DimSchemaResp dimensionDesc = new DimSchemaResp();
        dimensionDesc.setId(162L);
        dimensionDescs1.add(dimensionDesc);
        entityDesc.setEntityIds(dimensionDescs1);

        DimSchemaResp dimensionDesc2 = new DimSchemaResp();
        dimensionDesc2.setId(163L);
        dimensionDesc2.setBizName("song_name");
        dimensionDesc2.setName("歌曲名");

        EntityInternalDetail entityInternalDetailDesc = new EntityInternalDetail();
        entityInternalDetailDesc.setDimensionList(new ArrayList<>(Arrays.asList(dimensionDesc2)));
        MetricSchemaResp metricDesc = new MetricSchemaResp();
        metricDesc.setId(877L);
        metricDesc.setBizName("js_play_cnt");
        metricDesc.setName("结算播放量");
        entityInternalDetailDesc.setMetricList(new ArrayList<>(Arrays.asList(metricDesc)));
        entityDesc.setEntityInternalDetailDesc(entityInternalDetailDesc);

        chaConfigRichDesc.setEntity(entityDesc);
//        when(httpSemanticLayer.getChatConfigRichInfo(anyLong())).thenReturn(chaConfigRichDesc);
        DomainSchemaResp domainSchemaDesc = new DomainSchemaResp();
        domainSchemaDesc.setDimensions(dimensionDescs);
        domainSchemaDesc.setMetrics(metricDescs);
        when(httpSemanticLayer.getDomainSchemaInfo(anyLong())).thenReturn(domainSchemaDesc);

        DomainInfos domainInfos = new DomainInfos();
        when(SchemaInfoConverter.convert(httpSemanticLayer.getDomainSchemaInfo(anyList()))).thenReturn(domainInfos);

    }

    public static void getDomainExtendMock(ConfigServiceImpl configService) {
        DefaultMetricInfo defaultMetricInfo = new DefaultMetricInfo();
        defaultMetricInfo.setUnit(3);
        defaultMetricInfo.setPeriod(Constants.DAY);
        List<DefaultMetricInfo> defaultMetricInfos = new ArrayList<>();
        defaultMetricInfos.add(defaultMetricInfo);

        ChatConfigInfo chaConfigDesc = new ChatConfigInfo();
        chaConfigDesc.setDefaultMetrics(defaultMetricInfos);
        when(configService.fetchConfigByDomainId(anyLong())).thenReturn(chaConfigDesc);
    }

    //queryDimensionDescs

    public static void dimensionDescBuild(DimensionService dimensionService, List<DimensionResp> dimensionDescs) {
        when(dimensionService.getDimensions(anyList())).thenReturn(dimensionDescs);
    }

    public static void metricDescBuild(MetricService dimensionService, List<MetricResp> metricDescs) {
        when(dimensionService.getMetrics(anyList())).thenReturn(metricDescs);
    }

    public static DimSchemaResp getDimensionDesc(Long id, String bizName, String name) {
        DimSchemaResp dimensionDesc = new DimSchemaResp();
        dimensionDesc.setId(id);
        dimensionDesc.setName(name);
        dimensionDesc.setBizName(bizName);
        return dimensionDesc;
    }

    public static MetricSchemaResp getMetricDesc(Long id, String bizName, String name) {
        MetricSchemaResp dimensionDesc = new MetricSchemaResp();
        dimensionDesc.setId(id);
        dimensionDesc.setName(name);
        dimensionDesc.setBizName(bizName);
        return dimensionDesc;
    }
}
