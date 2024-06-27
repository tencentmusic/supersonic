package com.tencent.supersonic.headless.server.service;

import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.DataFormat;
import com.tencent.supersonic.common.pojo.enums.DataFormatTypeEnum;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.MeasureParam;
import com.tencent.supersonic.headless.api.pojo.MetricDefineByMeasureParams;
import com.tencent.supersonic.headless.api.pojo.RelateDimension;
import com.tencent.supersonic.headless.api.pojo.enums.MetricDefineType;
import com.tencent.supersonic.headless.api.pojo.enums.MetricType;
import com.tencent.supersonic.headless.api.pojo.request.MetricReq;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.server.facade.service.ChatQueryService;
import com.tencent.supersonic.headless.server.persistence.dataobject.MetricDO;
import com.tencent.supersonic.headless.server.persistence.repository.MetricRepository;
import com.tencent.supersonic.headless.server.utils.AliasGenerateHelper;
import com.tencent.supersonic.headless.server.utils.MetricConverter;
import com.tencent.supersonic.headless.server.web.service.CollectService;
import com.tencent.supersonic.headless.server.web.service.DataSetService;
import com.tencent.supersonic.headless.server.web.service.DimensionService;
import com.tencent.supersonic.headless.server.web.service.MetricService;
import com.tencent.supersonic.headless.server.web.service.ModelService;
import com.tencent.supersonic.headless.server.web.service.TagMetaService;
import com.tencent.supersonic.headless.server.web.service.impl.DataSetServiceImpl;
import com.tencent.supersonic.headless.server.web.service.impl.MetricServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MetricServiceImplTest {

    @Test
    void createMetric() throws Exception {
        MetricRepository metricRepository = Mockito.mock(MetricRepository.class);
        ModelService modelService = Mockito.mock(ModelService.class);
        MetricService metricService = mockMetricService(metricRepository, modelService);
        MetricReq metricReq = buildMetricReq();
        when(modelService.getModel(metricReq.getModelId())).thenReturn(mockModelResp());
        when(modelService.getModelByDomainIds(any())).thenReturn(Lists.newArrayList());
        MetricResp actualMetricResp = metricService.createMetric(metricReq, User.getFakeUser());
        MetricResp expectedMetricResp = buildExpectedMetricResp();
        Assertions.assertEquals(expectedMetricResp, actualMetricResp);
    }

    @Test
    void updateMetric() throws Exception {
        MetricRepository metricRepository = Mockito.mock(MetricRepository.class);
        ModelService modelService = Mockito.mock(ModelService.class);
        MetricService metricService = mockMetricService(metricRepository, modelService);
        MetricReq metricReq = buildMetricUpdateReq();
        when(modelService.getModel(metricReq.getModelId())).thenReturn(mockModelResp());
        when(modelService.getModelByDomainIds(any())).thenReturn(Lists.newArrayList());
        MetricDO metricDO = MetricConverter.convert2MetricDO(buildMetricReq());
        when(metricRepository.getMetricById(metricDO.getId())).thenReturn(metricDO);
        MetricResp actualMetricResp = metricService.updateMetric(metricReq, User.getFakeUser());
        MetricResp expectedMetricResp = buildExpectedMetricResp();
        Assertions.assertEquals(expectedMetricResp, actualMetricResp);
    }

    private MetricService mockMetricService(MetricRepository metricRepository,
            ModelService modelService) {
        AliasGenerateHelper aliasGenerateHelper = Mockito.mock(AliasGenerateHelper.class);
        CollectService collectService = Mockito.mock(CollectService.class);
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        DataSetService dataSetService = Mockito.mock(DataSetServiceImpl.class);
        DimensionService dimensionService = Mockito.mock(DimensionService.class);
        TagMetaService tagMetaService = Mockito.mock(TagMetaService.class);
        ChatQueryService chatQueryService = Mockito.mock(ChatQueryService.class);
        return new MetricServiceImpl(metricRepository, modelService, aliasGenerateHelper,
                collectService, dataSetService, eventPublisher, dimensionService,
                tagMetaService, chatQueryService);
    }

    private MetricReq buildMetricReq() {
        MetricReq metricReq = new MetricReq();
        metricReq.setId(1L);
        metricReq.setName("hr部门的访问次数");
        metricReq.setBizName("pv");
        metricReq.setDescription("SuperSonic的访问情况");
        metricReq.setAlias("pv");
        metricReq.setMetricDefineType(MetricDefineType.MEASURE);
        metricReq.setModelId(2L);
        metricReq.setDataFormatType(DataFormatTypeEnum.PERCENT.getName());
        DataFormat dataFormat = new DataFormat();
        dataFormat.setDecimalPlaces(3);
        dataFormat.setNeedMultiply100(false);
        metricReq.setDataFormat(dataFormat);
        MetricDefineByMeasureParams typeParams = new MetricDefineByMeasureParams();
        typeParams.setMeasures(Lists.newArrayList(
                new MeasureParam("s2_pv", "department='hr'"),
                new MeasureParam("s2_uv", "department='hr'")));
        typeParams.setExpr("s2_pv/s2_uv");
        metricReq.setMetricDefineByMeasureParams(typeParams);
        metricReq.setClassifications(Lists.newArrayList("核心指标"));
        metricReq.setRelateDimension(
                RelateDimension.builder().drillDownDimensions(Lists.newArrayList(
                        new DrillDownDimension(1L),
                        new DrillDownDimension(1L, false))
                ).build());
        metricReq.setSensitiveLevel(SensitiveLevelEnum.LOW.getCode());
        metricReq.setExt(new HashMap<>());
        return metricReq;
    }

    private MetricResp buildExpectedMetricResp() {
        MetricResp metricResp = new MetricResp();
        metricResp.setId(1L);
        metricResp.setName("hr部门的访问次数");
        metricResp.setBizName("pv");
        metricResp.setDescription("SuperSonic的访问情况");
        metricResp.setAlias("pv");
        metricResp.setMetricDefineType(MetricDefineType.MEASURE);
        metricResp.setModelId(2L);
        metricResp.setDataFormatType(DataFormatTypeEnum.PERCENT.getName());
        DataFormat dataFormat = new DataFormat();
        dataFormat.setDecimalPlaces(3);
        dataFormat.setNeedMultiply100(false);
        metricResp.setDataFormat(dataFormat);
        MetricDefineByMeasureParams typeParams = new MetricDefineByMeasureParams();
        typeParams.setMeasures(Lists.newArrayList(
                new MeasureParam("s2_pv", "department='hr'"),
                new MeasureParam("s2_uv", "department='hr'")));
        typeParams.setExpr("s2_pv/s2_uv");
        metricResp.setMetricDefineByMeasureParams(typeParams);
        metricResp.setClassifications("核心指标");
        metricResp.setRelateDimension(
                RelateDimension.builder().drillDownDimensions(Lists.newArrayList(
                        new DrillDownDimension(1L),
                        new DrillDownDimension(1L, false))
                ).build());
        metricResp.setSensitiveLevel(SensitiveLevelEnum.LOW.getCode());
        metricResp.setExt(new HashMap<>());
        metricResp.setTypeEnum(TypeEnums.METRIC);
        metricResp.setIsCollect(false);
        metricResp.setType(MetricType.DERIVED.name());
        metricResp.setStatus(StatusEnum.ONLINE.getCode());
        return metricResp;
    }

    private MetricReq buildMetricUpdateReq() {
        MetricReq metricReq = new MetricReq();
        metricReq.setId(1L);
        metricReq.setName("hr部门的访问次数");
        metricReq.setBizName("pv");
        metricReq.setMetricDefineType(MetricDefineType.MEASURE);
        MetricDefineByMeasureParams typeParams = new MetricDefineByMeasureParams();
        typeParams.setMeasures(Lists.newArrayList(
                new MeasureParam("s2_pv", "department='hr'"),
                new MeasureParam("s2_uv", "department='hr'")));
        typeParams.setExpr("s2_pv/s2_uv");
        metricReq.setMetricDefineByMeasureParams(typeParams);
        return metricReq;
    }

    private ModelResp mockModelResp() {
        ModelResp modelResp = new ModelResp();
        modelResp.setId(2L);
        modelResp.setDomainId(1L);
        return modelResp;
    }

}
