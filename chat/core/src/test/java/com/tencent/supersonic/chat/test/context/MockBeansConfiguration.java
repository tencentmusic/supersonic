package com.tencent.supersonic.chat.test.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.tencent.supersonic.chat.api.pojo.ChatContext;
import com.tencent.supersonic.chat.api.pojo.response.ChatConfigResp;
import com.tencent.supersonic.chat.config.DefaultMetric;
import com.tencent.supersonic.chat.config.DefaultMetricInfo;
import com.tencent.supersonic.chat.config.EntityInternalDetail;
import com.tencent.supersonic.chat.persistence.repository.impl.ChatContextRepositoryImpl;
import com.tencent.supersonic.chat.service.QueryService;
import com.tencent.supersonic.semantic.api.model.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.MetricSchemaResp;
import com.tencent.supersonic.chat.service.impl.ConfigServiceImpl;
import com.tencent.supersonic.chat.service.ChatService;
import com.tencent.supersonic.chat.persistence.mapper.ChatContextMapper;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tencent.supersonic.semantic.model.domain.pojo.DimensionFilter;
import com.tencent.supersonic.semantic.model.domain.pojo.MetaFilter;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MockBeansConfiguration {

    public static void getOrCreateContextMock(ChatService chatService) {
        ChatContext context = new ChatContext();
        context.setChatId(1);
        when(chatService.getOrCreateContext(1)).thenReturn(context);
    }

    public static void buildHttpSemanticServiceImpl(List<DimSchemaResp> dimensionDescs,
                                                    List<MetricSchemaResp> metricDescs) {
        DefaultMetric defaultMetricDesc = new DefaultMetric();
        defaultMetricDesc.setUnit(3);
        defaultMetricDesc.setPeriod(Constants.DAY);
        List<DimSchemaResp> dimensionDescs1 = new ArrayList<>();
        DimSchemaResp dimensionDesc = new DimSchemaResp();
        dimensionDesc.setId(162L);
        dimensionDescs1.add(dimensionDesc);

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

        ModelSchemaResp modelSchemaDesc = new ModelSchemaResp();
        modelSchemaDesc.setDimensions(dimensionDescs);
        modelSchemaDesc.setMetrics(metricDescs);

    }

    public static void getModelExtendMock(ConfigServiceImpl configService) {
        DefaultMetricInfo defaultMetricInfo = new DefaultMetricInfo();
        defaultMetricInfo.setUnit(3);
        defaultMetricInfo.setPeriod(Constants.DAY);
        List<DefaultMetricInfo> defaultMetricInfos = new ArrayList<>();
        defaultMetricInfos.add(defaultMetricInfo);

        ChatConfigResp chaConfigDesc = new ChatConfigResp();
        when(configService.fetchConfigByModelId(anyLong())).thenReturn(chaConfigDesc);
    }

    public static void dimensionDescBuild(DimensionService dimensionService, List<DimensionResp> dimensionDescs) {
        when(dimensionService.getDimensions(any(DimensionFilter.class))).thenReturn(dimensionDescs);
    }

    public static void metricDescBuild(MetricService metricService, List<MetricResp> metricDescs) {
        when(metricService.getMetrics(any(MetaFilter.class))).thenReturn(metricDescs);
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

    //queryDimensionDescs

    @Bean
    public ModelService getModelService() {
        return Mockito.mock(ModelService.class);
    }

    @Bean
    public ChatContextMapper getChatContextMapper() {
        return Mockito.mock(ChatContextMapper.class);
    }

    @Bean
    public ConfigServiceImpl getModelExtendService() {
        return Mockito.mock(ConfigServiceImpl.class);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
