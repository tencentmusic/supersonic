package com.tencent.supersonic.semantic.materialization.application;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.RecordInfo;
import com.tencent.supersonic.common.pojo.enums.StatusEnum;
import com.tencent.supersonic.common.pojo.enums.TypeEnums;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.jsqlparser.SqlParserSelectHelper;
import com.tencent.supersonic.semantic.api.materialization.enums.ElementFrequencyEnum;
import com.tencent.supersonic.semantic.api.materialization.enums.ElementTypeEnum;
import com.tencent.supersonic.semantic.api.materialization.pojo.MaterializationConfFilter;
import com.tencent.supersonic.semantic.api.materialization.pojo.MaterializationFilter;
import com.tencent.supersonic.semantic.api.materialization.request.MaterializationElementReq;
import com.tencent.supersonic.semantic.api.materialization.request.MaterializationReq;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationElementModelResp;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationElementResp;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationResp;
import com.tencent.supersonic.semantic.api.materialization.response.MaterializationSourceResp;
import com.tencent.supersonic.semantic.api.model.pojo.Measure;
import com.tencent.supersonic.semantic.api.model.pojo.SchemaItem;
import com.tencent.supersonic.semantic.api.model.request.ModelSchemaFilterReq;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.DimSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.MeasureResp;
import com.tencent.supersonic.semantic.api.model.response.MetricSchemaResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaResp;
import com.tencent.supersonic.semantic.materialization.domain.MaterializationConfService;
import com.tencent.supersonic.semantic.materialization.domain.pojo.Materialization;
import com.tencent.supersonic.semantic.materialization.domain.pojo.MaterializationElement;
import com.tencent.supersonic.semantic.materialization.domain.repository.MaterializationElementRepository;
import com.tencent.supersonic.semantic.materialization.domain.repository.MaterializationRepository;
import com.tencent.supersonic.semantic.materialization.domain.utils.MaterializationConverter;
import com.tencent.supersonic.semantic.materialization.domain.utils.MaterializationZipperUtils;
import com.tencent.supersonic.semantic.model.domain.DatasourceService;
import com.tencent.supersonic.semantic.model.domain.ModelService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Slf4j
@Service
public class MaterializationConfServiceImpl implements MaterializationConfService {

    private final MaterializationRepository materializationRepository;
    private final MaterializationElementRepository materializationElementRepository;
    private final ModelService modelService;
    private final DatasourceService datasourceService;
    private final MaterializationZipperUtils materializationZipperUtils;
    private String typeAndIdSplit = "_";

    public MaterializationConfServiceImpl(MaterializationRepository materializationRepository,
                                          MaterializationElementRepository materializationElementRepository,
                                          ModelService modelService, DatasourceService datasourceService,
                                          MaterializationZipperUtils materializationZipperUtils) {
        this.materializationRepository = materializationRepository;
        this.materializationElementRepository = materializationElementRepository;
        this.modelService = modelService;
        this.datasourceService = datasourceService;
        this.materializationZipperUtils = materializationZipperUtils;
    }

    @Override
    public Boolean addMaterializationConf(MaterializationReq materializationReq, User user) {
        log.info("materializationReq:{}, user:{}", JsonUtil.toString(materializationReq), JsonUtil.toString(user));
        Materialization materialization = MaterializationConverter.materializationReq2Bean(materializationReq);
        RecordInfo recordInfo = new RecordInfo().createdBy(user.getName());
        BeanUtils.copyProperties(recordInfo, materialization);
        return materializationRepository.insert(materialization);
    }

    @Override
    public Boolean updateMaterializationConf(MaterializationReq materializationReq, User user) {
        Materialization materialization = MaterializationConverter.materializationReq2Bean(materializationReq);
        RecordInfo recordInfo = new RecordInfo().updatedBy(user.getName());
        BeanUtils.copyProperties(recordInfo, materialization);
        return materializationRepository.update(materialization);
    }

    @Override
    public List<MaterializationResp> getMaterializationResp(MaterializationFilter filter, User user) {
        return materializationRepository.getMaterializationResp(filter);
    }

    public MaterializationResp getMaterializationRespById(Long materializationId, User user) {
        MaterializationResp materializationResp = new MaterializationResp();
        MaterializationConfFilter filter = new MaterializationConfFilter();
        filter.setMaterializationId(materializationId);
        filter.setContainElements(true);
        List<MaterializationResp> materializationRespList = queryMaterializationConf(filter, user);
        if (CollectionUtils.isEmpty(materializationRespList)) {
            return materializationResp;
        }
        return materializationRespList.get(0);
    }


    @Override
    public Boolean addMaterializationElementConf(MaterializationElementReq materializationElementReq, User user) {
        log.info("materializationElementReq:{}, user:{}", JsonUtil.toString(materializationElementReq),
                JsonUtil.toString(user));
        MaterializationElement materializationElement = MaterializationConverter
                .materializationElementReq2Bean(materializationElementReq);
        RecordInfo recordInfo = new RecordInfo().createdBy(user.getName());
        BeanUtils.copyProperties(recordInfo, materializationElement);
        return materializationElementRepository.insert(materializationElement);
    }

    @Override
    public Boolean updateMaterializationElementConf(MaterializationElementReq materializationElementReq, User user) {
        MaterializationElement materializationElement = MaterializationConverter
                .materializationElementReq2Bean(materializationElementReq);
        RecordInfo recordInfo = new RecordInfo().updatedBy(user.getName());
        BeanUtils.copyProperties(recordInfo, materializationElement);
        return materializationElementRepository.update(materializationElement);
    }

    @Override
    public List<MaterializationResp> queryMaterializationConf(MaterializationConfFilter filter, User user) {
        MaterializationFilter materializationFilter = MaterializationFilter.builder().build();
        BeanUtils.copyProperties(filter, materializationFilter);
        List<MaterializationResp> materializationRespList = getMaterializationResp(materializationFilter, user);
        if (!CollectionUtils.isEmpty(materializationRespList) && filter.getContainElements()) {
            Map<String, SchemaItem> keyAndSchemaItemPair = generateSchemaItem(filter, user);
            materializationRespList.stream().forEach(materializationResp -> {
                filter.setMaterializationId(materializationResp.getId());
                List<MaterializationElementResp> materializationElementRespList = materializationElementRepository
                        .getMaterializationElementResp(filter);
                fillElementInfo(materializationElementRespList, keyAndSchemaItemPair);
                materializationResp.setMaterializationElementRespList(materializationElementRespList);
            });
        }

        return materializationRespList;
    }

    private void fillElementInfo(List<MaterializationElementResp> materializationElementRespList,
                                 Map<String, SchemaItem> keyAndSchemaItemPair) {
        if (CollectionUtils.isEmpty(materializationElementRespList) || Objects.isNull(keyAndSchemaItemPair)) {
            return;
        }
        materializationElementRespList.stream().forEach(materializationElementResp -> {
            String key = materializationElementResp.getType() + typeAndIdSplit + materializationElementResp.getId();
            SchemaItem schemaItem = keyAndSchemaItemPair.getOrDefault(key, null);
            if (Objects.nonNull(schemaItem)) {
                materializationElementResp.setBizName(schemaItem.getBizName());
            }
        });
    }

    private Map<String, SchemaItem> generateSchemaItem(MaterializationConfFilter filter, User user) {
        return generateSchemaItem(filter);
    }

    private Map<String, SchemaItem> generateSchemaItem(MaterializationConfFilter filter) {
        Map<String, SchemaItem> keyAndSchemaItemPair = new HashMap<>();

        ModelSchemaFilterReq modelFilter = new ModelSchemaFilterReq();
        List<Long> modelIds = new ArrayList<>();
        if (Objects.nonNull(filter.getModelId())) {
            modelIds.add(filter.getModelId());
        }
        List<ModelSchemaResp> modelSchemaRespList = modelService.fetchModelSchema(modelFilter);
        if (!CollectionUtils.isEmpty(modelSchemaRespList)) {
            modelSchemaRespList.stream().forEach(modelSchemaResp -> {
                List<DimSchemaResp> dimensions = modelSchemaResp.getDimensions();
                List<MetricSchemaResp> metrics = modelSchemaResp.getMetrics();
                if (!CollectionUtils.isEmpty(dimensions)) {
                    dimensions.stream().forEach(dimSchemaResp -> {
                        SchemaItem schemaItem = new SchemaItem();
                        BeanUtils.copyProperties(dimSchemaResp, schemaItem);
                        String key = TypeEnums.DIMENSION.name() + typeAndIdSplit + dimSchemaResp.getId();
                        keyAndSchemaItemPair.put(key, schemaItem);
                    });
                }

                if (!CollectionUtils.isEmpty(metrics)) {
                    metrics.stream().forEach(metricSchemaResp -> {
                        SchemaItem schemaItem = new SchemaItem();
                        BeanUtils.copyProperties(metricSchemaResp, schemaItem);
                        String key = TypeEnums.METRIC.name() + typeAndIdSplit + metricSchemaResp.getId();
                        keyAndSchemaItemPair.put(key, schemaItem);
                    });
                }
            });
        }
        return keyAndSchemaItemPair;
    }

    @Override
    public List<MaterializationResp> getMaterializationByModel(Long modelId) {
        MaterializationFilter filter = new MaterializationConfFilter();
        filter.setModelId(modelId);
        List<MaterializationResp> materializationRespList = materializationRepository.getMaterializationResp(filter);
        MaterializationConfFilter materializationConfFilter = new MaterializationConfFilter();
        if (!CollectionUtils.isEmpty(materializationRespList)) {
            materializationRespList.stream().forEach(materializationResp -> {
                materializationConfFilter.setMaterializationId(materializationResp.getId());
                List<MaterializationElementResp> materializationElementRespList = materializationElementRepository
                        .getMaterializationElementResp(materializationConfFilter);
                materializationResp.setMaterializationElementRespList(materializationElementRespList);
            });
        }
        return materializationRespList;
    }

    @Override
    public List<Long> getMaterializationByTable(Long databaseId, String destinationTable) {
        MaterializationFilter filter = new MaterializationConfFilter();
        filter.setDestinationTable(destinationTable);
        filter.setDatabaseId(databaseId);
        List<MaterializationResp> materializationRespList = materializationRepository.getMaterializationResp(filter);
        if (!CollectionUtils.isEmpty(materializationRespList)) {
            return materializationRespList.stream().map(m -> m.getId()).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public String generateCreateSql(Long materializationId, User user) {
        MaterializationConfFilter filter = new MaterializationConfFilter();
        filter.setMaterializationId(materializationId);
        filter.setContainElements(true);
        List<MaterializationResp> materializationRespList = queryMaterializationConf(filter, user);
        if (CollectionUtils.isEmpty(materializationRespList)) {
            log.warn("materializationRespList is empty, materializationId:{}", materializationId);
            return "";
        }
        // 获取db 连接信息
        MaterializationResp materializationResp = materializationRespList.get(0);

        return generateCreateSql(materializationResp);
    }

    private String generateCreateSql(MaterializationResp materializationResp) {
        return materializationZipperUtils.generateCreateSql(materializationResp);
    }

    @Override
    public Boolean initMaterializationElementConf(MaterializationConfFilter filter, User user) {
        Long materializationId = filter.getMaterializationId();
        MaterializationResp materializationResp = getMaterializationRespById(materializationId, user);
        Long modelId = materializationResp.getModelId();
        ModelSchemaResp modelSchemaResp = modelService.fetchSingleModelSchema(modelId);

        doDimensionMaterializationLogic(modelSchemaResp.getDimensions(), materializationResp, user);

        return true;
    }

    private void doDimensionMaterializationLogic(List<DimSchemaResp> dimensions,
                                                 MaterializationResp materializationResp, User user) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return;
        }
        Long materializationId = materializationResp.getId();
        cleanMaterializationElement(materializationId, user);
        for (DimSchemaResp dimSchemaResp : dimensions) {
            MaterializationElementReq materializationElementReq = MaterializationElementReq.builder()
                    .id(dimSchemaResp.getId())
                    .type(TypeEnums.DIMENSION)
                    .materializationId(materializationId)
                    .elementType(ElementTypeEnum.VARCHAR)
                    .frequency(ElementFrequencyEnum.LOW)
                    .status(StatusEnum.ONLINE)
                    .description(dimSchemaResp.getDescription())
                    .build();

            addMaterializationElementConf(materializationElementReq, user);
        }
        MaterializationConfFilter filter = new MaterializationConfFilter();
        filter.setMaterializationId(materializationId);

        MaterializationResp materializationRespNew = getMaterializationRespById(materializationId, user);
        String createSql = generateCreateSql(materializationRespNew);
        log.info("createSql:{}", createSql);

    }

    private Boolean cleanMaterializationElement(Long materializationId, User user) {
        log.info("cleanMaterializationElement materializationId:{}", materializationId);
        return materializationElementRepository.cleanMaterializationElement(materializationId);
    }


    @Override
    public List<MaterializationElementModelResp> getMaterializationElementModels(Long materializationId, User user) {
        MaterializationResp materializationResp = materializationRepository.getMaterialization(materializationId);
        MaterializationConfFilter filter = new MaterializationConfFilter();
        filter.setMaterializationId(materializationId);
        List<MaterializationElementResp> materializationElementRespList = materializationElementRepository
                .getMaterializationElementResp(filter);
        List<MaterializationElementModelResp> materializationElementModelRespList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(materializationElementRespList)) {
            ModelSchemaFilterReq modelFilter = new ModelSchemaFilterReq();
            modelFilter.setModelIds(Arrays.asList(materializationResp.getModelId()));
            List<ModelSchemaResp> modelSchemaRespList = modelService.fetchModelSchema(modelFilter);
            List<MeasureResp> measureRespList = datasourceService.getMeasureListOfModel(
                    materializationResp.getModelId());
            Map<String, DimSchemaResp> dimSchemaRespMap = new HashMap<>();
            Map<String, MetricSchemaResp> metricSchemaRespHashMap = new HashMap<>();
            if (!CollectionUtils.isEmpty(modelSchemaRespList)) {
                modelSchemaRespList.stream().forEach(modelSchemaResp -> {
                    List<DimSchemaResp> dimensions = modelSchemaResp.getDimensions();
                    List<MetricSchemaResp> metrics = modelSchemaResp.getMetrics();
                    if (!CollectionUtils.isEmpty(dimensions)) {
                        dimensions.stream().forEach(dimSchemaResp -> {
                            String key = TypeEnums.DIMENSION.name() + typeAndIdSplit + dimSchemaResp.getId();
                            dimSchemaRespMap.put(key, dimSchemaResp);
                        });
                    }
                    if (!CollectionUtils.isEmpty(metrics)) {
                        metrics.stream().forEach(metricSchemaResp -> {
                            String key = TypeEnums.METRIC.name() + typeAndIdSplit + metricSchemaResp.getId();
                            metricSchemaRespHashMap.put(key, metricSchemaResp);
                        });
                    }
                });
            }
            materializationElementRespList.stream().forEach(materializationElementResp -> {
                String key = materializationElementResp.getType() + typeAndIdSplit + materializationElementResp.getId();
                DimSchemaResp schemaItem = dimSchemaRespMap.getOrDefault(key, null);
                MaterializationElementModelResp materializationElementModelResp = MaterializationElementModelResp
                        .builder()
                        .type(materializationElementResp.getType())
                        .id(materializationElementResp.getId())
                        .build();
                if (Objects.nonNull(schemaItem)) {
                    materializationElementModelResp.setBizName(schemaItem.getBizName());
                    materializationElementModelResp.setExpr(schemaItem.getExpr());
                    materializationElementModelRespList.add(materializationElementModelResp);
                } else {
                    MetricSchemaResp metricSchemaResp = metricSchemaRespHashMap.getOrDefault(key, null);
                    if (Objects.nonNull(metricSchemaResp)) {
                        materializationElementModelResp.setBizName(metricSchemaResp.getBizName());
                        materializationElementModelResp.setExpr(metricSchemaResp.getTypeParams().getExpr());
                        materializationElementModelResp.setMeasures(metricSchemaResp.getTypeParams().getMeasures());
                        materializationElementModelResp.getMeasures().forEach(m -> {
                            m.setExpr(getDataSourceMeasure(measureRespList, m.getBizName()));
                        });
                        materializationElementModelRespList.add(materializationElementModelResp);
                    }
                }
            });
        }
        return materializationElementModelRespList;
    }

    @Override
    public List<MaterializationSourceResp> getMaterializationSourceResp(
            Long materializationId) {

        MaterializationResp materializationResp = materializationRepository.getMaterialization(
                materializationId);
        Long modelId = materializationResp.getModelId();
        List<DatasourceResp> modelDataSources = datasourceService.getDatasourceList(modelId);

        Set<Long> dataSourceIds = new HashSet<>();
        Map<Long, Map<Long, String>> dataSourceDimensions = new HashMap<>();
        Map<Long, Map<Long, String>> dataSourceMetrics = new HashMap<>();
        MaterializationConfFilter materializationConfFilter = new MaterializationConfFilter();
        materializationConfFilter.setMaterializationId(materializationId);
        List<MaterializationElementResp> materializationElementRespList = materializationElementRepository
                .getMaterializationElementResp(materializationConfFilter);
        if (!CollectionUtils.isEmpty(materializationElementRespList)) {
            ModelSchemaFilterReq modelSchemaFilterReq = new ModelSchemaFilterReq();
            modelSchemaFilterReq.setModelIds(Arrays.asList(modelId));
            List<ModelSchemaResp> modelSchemaRespList = modelService.fetchModelSchema(modelSchemaFilterReq);
            List<MeasureResp> measureRespList = datasourceService.getMeasureListOfModel(modelId);
            Set<Long> dimensionIds = new HashSet<>();
            Set<Long> metricIds = new HashSet<>();
            materializationElementRespList.stream().forEach(e -> {
                if (e.getType().equals(TypeEnums.DIMENSION)) {
                    dimensionIds.add(e.getId());
                }
                if (e.getType().equals(TypeEnums.METRIC)) {
                    metricIds.add(e.getId());
                }
            });
            modelSchemaRespList.stream().forEach(m -> {
                m.getDimensions().stream().filter(mm -> dimensionIds.contains(mm.getId())).forEach(mm -> {
                    if (!dataSourceDimensions.containsKey(mm.getDatasourceId())) {
                        dataSourceDimensions.put(mm.getDatasourceId(), new HashMap<>());
                    }
                    dataSourceDimensions.get(mm.getDatasourceId()).put(mm.getId(), mm.getBizName());
                    dataSourceIds.add(mm.getDatasourceId());
                });
                m.getMetrics().stream().filter(mm -> metricIds.contains(mm.getId())).forEach(mm -> {
                    Long sourceId = 0L;
                    for (Measure measure : mm.getTypeParams().getMeasures()) {
                        sourceId = getSourceIdByMeasure(measureRespList, measure.getBizName());
                        if (sourceId > 0) {
                            break;
                        }
                    }
                    if (sourceId > 0) {
                        if (!dataSourceMetrics.containsKey(sourceId)) {
                            dataSourceMetrics.put(sourceId, new HashMap<>());
                        }
                        dataSourceMetrics.get(sourceId).put(mm.getId(), mm.getBizName());
                        dataSourceIds.add(sourceId);
                    }
                });
            });
        }
        List<MaterializationSourceResp> materializationSourceResps = new ArrayList<>();
        for (Long sourceId : dataSourceIds) {
            Optional<DatasourceResp> datasourceResp = modelDataSources.stream().filter(d -> d.getId().equals(sourceId))
                    .findFirst();
            if (datasourceResp.isPresent()) {
                MaterializationSourceResp materializationSourceResp = MaterializationSourceResp.builder()
                        .dataSourceId(sourceId)
                        .materializationId(materializationId)
                        .modelId(modelId)
                        .depends(datasourceResp.get().getDepends())
                        .materializedType(materializationResp.getMaterializedType())
                        .entities(materializationResp.getEntities())
                        .dateInfo(materializationResp.getDateInfo())
                        .updateCycle(materializationResp.getUpdateCycle())
                        .build();
                setDataSourceDb(datasourceResp.get(), materializationSourceResp);
                materializationSourceResp.setMetrics(
                        dataSourceMetrics.containsKey(sourceId) ? dataSourceMetrics.get(sourceId)
                                : new HashMap<>());
                materializationSourceResp.setDimensions(
                        dataSourceDimensions.containsKey(sourceId) ? dataSourceDimensions.get(sourceId)
                                : new HashMap<>());
                materializationSourceResps.add(materializationSourceResp);
            }
        }
        return materializationSourceResps;
    }

    public Long getSourceIdByMeasure(List<MeasureResp> measureRespList, String bizName) {
        if (!CollectionUtils.isEmpty(measureRespList)) {
            Optional<MeasureResp> measure = measureRespList.stream()
                    .filter(m -> m.getBizName().equalsIgnoreCase(bizName)).findFirst();
            if (measure.isPresent()) {
                return measure.get().getDatasourceId();
            }
        }
        return 0L;
    }

    private void setDataSourceDb(DatasourceResp datasourceResp, MaterializationSourceResp materializationSourceResp) {
        if (Objects.nonNull(datasourceResp.getDatasourceDetail())) {
            String dbTable = "";
            if (Objects.nonNull(datasourceResp.getDatasourceDetail().getTableQuery())
                    && !datasourceResp.getDatasourceDetail().getTableQuery().isEmpty()) {
                dbTable = datasourceResp.getDatasourceDetail().getTableQuery();
            }
            if (Objects.nonNull(datasourceResp.getDatasourceDetail().getSqlQuery())
                    && !datasourceResp.getDatasourceDetail().getSqlQuery().isEmpty()) {
                dbTable = SqlParserSelectHelper.getDbTableName(datasourceResp.getDatasourceDetail().getSqlQuery());
            }
            if (!dbTable.isEmpty()) {
                String[] db = dbTable.split("\\.");
                if (db.length > 1) {
                    materializationSourceResp.setSourceDb(db[0]);
                    materializationSourceResp.setSourceTable(db[1]);
                }
            }
        }
    }


    private String getDataSourceMeasure(List<MeasureResp> measureRespList, String bizName) {
        if (!CollectionUtils.isEmpty(measureRespList)) {
            Optional<MeasureResp> measure = measureRespList.stream()
                    .filter(m -> m.getBizName().equalsIgnoreCase(bizName)).findFirst();
            if (measure.isPresent()) {
                return measure.get().getExpr();
            }
        }
        return "";
    }


}