package com.tencent.supersonic.semantic.query.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.materialization.request.MaterializationSourceReq;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationSourceResp;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.request.ModelSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.response.MeasureResp;
import com.tencent.supersonic.semantic.api.model.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.api.query.request.MetricReq;
import com.tencent.supersonic.semantic.materialization.domain.MaterializationConfService;
import com.tencent.supersonic.semantic.model.domain.Catalog;
import com.tencent.supersonic.semantic.model.domain.DatasourceService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.query.persistence.pojo.QueryStatement;
import com.tencent.supersonic.semantic.query.utils.QueryStructUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service("MaterializationService")
@Slf4j
public class MaterializationServiceImpl implements MaterializationService {

    protected final MaterializationConfService materializationConfService;
    protected final ModelService modelService;
    protected final DatasourceService datasourceService;
    protected final Catalog catalog;
    protected final QueryStructUtils queryStructUtils;
    protected final QueryService queryService;

    public MaterializationServiceImpl(
            MaterializationConfService materializationConfService,
            ModelService modelService, DatasourceService datasourceService,
            Catalog catalog, QueryStructUtils queryStructUtils,
            QueryService queryService) {
        this.materializationConfService = materializationConfService;
        this.modelService = modelService;
        this.datasourceService = datasourceService;
        this.catalog = catalog;
        this.queryStructUtils = queryStructUtils;
        this.queryService = queryService;
    }

    @Override
    public MaterializationSourceResp getMaterializationDataSource(MaterializationSourceReq materializationSourceReq,
            User user) throws Exception {

        if (materializationSourceReq.getMaterializationId() <= 0 || materializationSourceReq.getDataSourceId() <= 0) {
            throw new Exception("MaterializationId and DataSourceId are must");
        }
        Long materializationId = materializationSourceReq.getMaterializationId();
        List<MaterializationSourceResp> materializationList = materializationConfService.getMaterializationSourceResp(
                materializationId);
        if (!CollectionUtils.isEmpty(materializationList)) {
            Optional<MaterializationSourceResp> materializationSourceRespOpt = materializationList.stream()
                    .filter(m -> m.getDataSourceId().equals(materializationSourceReq.getDataSourceId())).findFirst();
            if (materializationSourceRespOpt.isPresent()) {
                MaterializationSourceResp materializationSourceResp = materializationSourceRespOpt.get();
                Set<String> dimensionFields = new HashSet<>();
                Set<String> metricFields = new HashSet<>();
                ModelSchemaFilterReq modelFilter = new ModelSchemaFilterReq();
                modelFilter.setModelIds(Arrays.asList(materializationSourceResp.getModelId()));
                List<ModelSchemaResp> modelSchemaRespList = modelService.fetchModelSchema(modelFilter);
                List<MeasureResp> measureRespList = datasourceService.getMeasureListOfModel(
                        materializationSourceResp.getModelId());
                modelSchemaRespList.stream().forEach(m -> {
                    m.getDimensions().stream()
                            .filter(mm -> mm.getDatasourceId().equals(materializationSourceReq.getDataSourceId())
                                    && materializationSourceResp.getDimensions().keySet().contains(mm.getId())
                            ).forEach(mm -> {
                                dimensionFields.add(mm.getBizName());
                            });
                    for (MetricSchemaResp metricSchemaResp : m.getMetrics()) {
                        if (!materializationSourceResp.getMetrics().keySet().contains(metricSchemaResp.getId())) {
                            continue;
                        }
                        Long dataSourceId = 0L;
                        for (Measure measure : metricSchemaResp.getTypeParams().getMeasures()) {
                            dataSourceId = materializationConfService.getSourceIdByMeasure(measureRespList,
                                    measure.getBizName());
                            if (dataSourceId > 0) {
                                break;
                            }
                        }
                        if (dataSourceId.equals(materializationSourceReq.getDataSourceId())) {
                            metricFields.addAll(getMetric(metricSchemaResp));
                        }
                    }
                });
                if (!dimensionFields.isEmpty() || !metricFields.isEmpty()) {
                    materializationSourceResp.setFields(new ArrayList<>(dimensionFields));
                    materializationSourceResp.getFields().addAll(metricFields);

                    MetricReq metricReq = new MetricReq();
                    metricReq.setRootPath(catalog.getModelFullPath(materializationSourceResp.getModelId()));
                    metricReq.setMetrics(new ArrayList<>(metricFields));
                    metricReq.setDimensions(new ArrayList<>(dimensionFields));
                    metricReq.getDimensions().add(materializationSourceResp.getDateInfo());
                    metricReq.getDimensions().add(materializationSourceResp.getEntities());
                    metricReq.setNativeQuery(true);
                    if (CollectionUtils.isEmpty(metricReq.getMetrics())) {
                        String internalMetricName = queryStructUtils.generateInternalMetricName(
                                materializationSourceResp.getModelId(), metricReq.getDimensions());
                        metricReq.getMetrics().add(internalMetricName);
                    }
                    try {
                        QueryStatement queryStatement = queryService.parseMetricReq(metricReq);
                        if (queryStatement.isOk()) {
                            materializationSourceResp.setSql(queryStatement.getSql());
                        }
                    } catch (Exception e) {
                        log.error("getMaterializationDataSource sql error [{}]", e);
                    }
                }
                return materializationSourceResp;
            }
        }
        throw new Exception("cant find MaterializationSource");
    }


    protected List<String> getMetric(MetricSchemaResp metricSchemaResp) {
        if (Objects.nonNull(metricSchemaResp.getTypeParams()) && metricSchemaResp.getTypeParams().getExpr()
                .contains(queryStructUtils.getVariablePrefix())) {
            return metricSchemaResp.getTypeParams().getMeasures().stream().map(m -> m.getBizName())
                    .collect(Collectors.toList());
        }
        return Arrays.asList(metricSchemaResp.getBizName());
    }
}
