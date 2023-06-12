package com.tencent.supersonic.semantic.query.application;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.DatasourceYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.DimensionTimeTypeParamsTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.IdentifyYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.MeasureYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.MetricTypeParamsYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.MetricYamlTpl;
import com.tencent.supersonic.semantic.core.domain.DatasourceService;
import com.tencent.supersonic.semantic.core.domain.DomainService;
import com.tencent.supersonic.semantic.query.domain.parser.SemanticSchemaManager;
import com.tencent.supersonic.semantic.query.domain.parser.dsl.DataSource;
import com.tencent.supersonic.semantic.query.domain.parser.dsl.Dimension;
import com.tencent.supersonic.semantic.query.domain.parser.dsl.DimensionTimeTypeParams;
import com.tencent.supersonic.semantic.query.domain.parser.dsl.Identify;
import com.tencent.supersonic.semantic.query.domain.parser.dsl.Measure;
import com.tencent.supersonic.semantic.query.domain.parser.dsl.Metric;
import com.tencent.supersonic.semantic.query.domain.parser.dsl.MetricTypeParams;
import com.tencent.supersonic.semantic.query.domain.parser.dsl.SemanticModel;
import com.tencent.supersonic.semantic.query.domain.parser.schema.SemanticSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service("SemanticSchemaManager")
public class SemanticSchemaManagerImpl implements SemanticSchemaManager {

    @Autowired
    private LoadingCache<String, SemanticModel> loadingCache;

    private final DatasourceService datasourceService;
    private final DomainService domainService;


    public SemanticSchemaManagerImpl(DatasourceService datasourceService,
            DomainService domainService) {
        this.datasourceService = datasourceService;
        this.domainService = domainService;
    }


    @Override
    public SemanticModel reload(String rootPath) {
        SemanticModel semanticModel = new SemanticModel();
        semanticModel.setRootPath(rootPath);
        Map<Long, String> domainFullPathMap = domainService.getDomainFullPath();
        log.info("domainFullPathMap {}", domainFullPathMap);
        Set<Long> domainIds = domainFullPathMap.entrySet().stream().filter(e -> e.getValue().startsWith(rootPath))
                .map(e -> e.getKey()).collect(Collectors.toSet());
        if (domainIds.isEmpty()) {
            log.error("get domainId empty {}", rootPath);
            return semanticModel;
        }
        Map<String, List<DimensionYamlTpl>> dimensionYamlTpls = new HashMap<>();
        List<DatasourceYamlTpl> datasourceYamlTpls = new ArrayList<>();
        List<MetricYamlTpl> metricYamlTpls = new ArrayList<>();
        datasourceService.getModelYamlTplByDomainIds(domainIds, dimensionYamlTpls, datasourceYamlTpls, metricYamlTpls);
        if (!datasourceYamlTpls.isEmpty()) {
            Map<String, DataSource> dataSourceMap = datasourceYamlTpls.stream().map(d -> getDatasource(d))
                    .collect(Collectors.toMap(DataSource::getName, item -> item));
            semanticModel.setDatasourceMap(dataSourceMap);
        }
        if (!dimensionYamlTpls.isEmpty()) {
            Map<String, List<Dimension>> dimensionMap = new HashMap<>();
            for (Map.Entry<String, List<DimensionYamlTpl>> entry : dimensionYamlTpls.entrySet()) {
                dimensionMap.put(entry.getKey(), getDimensions(entry.getValue()));
            }
            semanticModel.setDimensionMap(dimensionMap);
        }
        if (!metricYamlTpls.isEmpty()) {
            semanticModel.setMetrics(getMetrics(metricYamlTpls));
        }
        return semanticModel;
    }

    //private Map<String, SemanticSchema> semanticSchemaMap = new HashMap<>();
    @Override
    public SemanticModel get(String rootPath) throws Exception {
        rootPath = formatKey(rootPath);
        SemanticModel schema = loadingCache.get(rootPath);
        if (schema == null) {
            return null;
        }
        return schema;
    }

    public static List<Metric> getMetrics(final List<MetricYamlTpl> t) {
        return getMetricsByMetricYamlTpl(t);
    }


    public static List<Dimension> getDimensions(final List<DimensionYamlTpl> t) {
        return getDimension(t);
    }


    public static DataSource getDatasource(final DatasourceYamlTpl d) {
        DataSource datasource = new DataSource();
        datasource.setSqlQuery(d.getSqlQuery());
        datasource.setName(d.getName());
        datasource.setSourceId(d.getSourceId());
        datasource.setTableQuery(d.getTableQuery());

        datasource.setIdentifiers(getIdentify(d.getIdentifiers()));
        datasource.setDimensions(getDimensions(d.getDimensions()));
        datasource.setMeasures(getMeasures(d.getMeasures()));
        return datasource;
    }

    private static List<Metric> getMetricsByMetricYamlTpl(List<MetricYamlTpl> metricYamlTpls) {
        List<Metric> metrics = new ArrayList<>();
        for (MetricYamlTpl metricYamlTpl : metricYamlTpls) {
            Metric metric = new Metric();
            metric.setMetricTypeParams(getMetricTypeParams(metricYamlTpl.getTypeParams()));
            metric.setOwners(metricYamlTpl.getOwners());
            metric.setType(metricYamlTpl.getType());
            metric.setName(metricYamlTpl.getName());
            metrics.add(metric);
        }
        return metrics;
    }

    private static MetricTypeParams getMetricTypeParams(MetricTypeParamsYamlTpl metricTypeParamsYamlTpl) {
        MetricTypeParams metricTypeParams = new MetricTypeParams();
        metricTypeParams.setExpr(metricTypeParamsYamlTpl.getExpr());
        metricTypeParams.setMeasures(getMeasures(metricTypeParamsYamlTpl.getMeasures()));
        return metricTypeParams;
    }

    private static List<Measure> getMeasures(List<MeasureYamlTpl> measureYamlTpls) {
        List<Measure> measures = new ArrayList<>();
        for (MeasureYamlTpl measureYamlTpl : measureYamlTpls) {
            Measure measure = new Measure();
            measure.setCreateMetric(measureYamlTpl.getCreateMetric());
            measure.setExpr(measureYamlTpl.getExpr());
            measure.setAgg(measureYamlTpl.getAgg());
            measure.setName(measureYamlTpl.getName());
            measure.setAlias(measureYamlTpl.getAlias());
            measure.setConstraint(measureYamlTpl.getConstraint());
            measures.add(measure);
        }
        return measures;
    }

    private static List<Dimension> getDimension(List<DimensionYamlTpl> dimensionYamlTpls) {
        List<Dimension> dimensions = new ArrayList<>();
        for (DimensionYamlTpl dimensionYamlTpl : dimensionYamlTpls) {
            Dimension dimension = new Dimension();
            dimension.setType(dimensionYamlTpl.getType());
            dimension.setExpr(dimensionYamlTpl.getExpr());
            dimension.setName(dimensionYamlTpl.getName());
            dimension.setOwners(dimensionYamlTpl.getOwners());
            dimension.setDimensionTimeTypeParams(getDimensionTimeTypeParams(dimensionYamlTpl.getTypeParams()));
            dimensions.add(dimension);
        }
        return dimensions;
    }

    private static DimensionTimeTypeParams getDimensionTimeTypeParams(
            DimensionTimeTypeParamsTpl dimensionTimeTypeParamsTpl) {
        DimensionTimeTypeParams dimensionTimeTypeParams = new DimensionTimeTypeParams();
        if (dimensionTimeTypeParamsTpl != null) {
            dimensionTimeTypeParams.setTimeGranularity(dimensionTimeTypeParamsTpl.getTimeGranularity());
            dimensionTimeTypeParams.setIsPrimary(dimensionTimeTypeParamsTpl.getIsPrimary());
        }
        return dimensionTimeTypeParams;
    }

    private static List<Identify> getIdentify(List<IdentifyYamlTpl> identifyYamlTpls) {
        List<Identify> identifies = new ArrayList<>();
        for (IdentifyYamlTpl identifyYamlTpl : identifyYamlTpls) {
            Identify identify = new Identify();
            identify.setType(identifyYamlTpl.getType());
            identify.setName(identifyYamlTpl.getName());
            identifies.add(identify);
        }
        return identifies;
    }


    public static void update(SemanticSchema schema, List<Metric> metric) throws Exception {
        if (schema != null) {
            updateMetric(metric, schema.getMetrics());
        }
    }

    public static void update(SemanticSchema schema, DataSource datasourceYamlTpl) throws Exception {
        if (schema != null) {
            String dataSourceName = datasourceYamlTpl.getName();
            Optional<Map.Entry<String, DataSource>> datasourceYamlTplMap = schema.getDatasource().entrySet().stream()
                    .filter(t -> t.getKey().equalsIgnoreCase(dataSourceName)).findFirst();
            if (datasourceYamlTplMap.isPresent()) {
                datasourceYamlTplMap.get().setValue(datasourceYamlTpl);
            } else {
                schema.getDatasource().put(dataSourceName, datasourceYamlTpl);
            }
        }
    }

    public static void update(SemanticSchema schema, String datasourceBizName, List<Dimension> dimensionYamlTpls)
            throws Exception {
        if (schema != null) {
            Optional<Map.Entry<String, List<Dimension>>> datasourceYamlTplMap = schema.getDimension().entrySet()
                    .stream().filter(t -> t.getKey().equalsIgnoreCase(datasourceBizName)).findFirst();
            if (datasourceYamlTplMap.isPresent()) {
                updateDimension(dimensionYamlTpls, datasourceYamlTplMap.get().getValue());
            } else {
                List<Dimension> dimensions = new ArrayList<>();
                updateDimension(dimensionYamlTpls, dimensions);
                schema.getDimension().put(datasourceBizName, dimensions);
            }
        }
    }

    private static void updateDimension(List<Dimension> dimensionYamlTpls, List<Dimension> dimensions) {
        if (CollectionUtils.isEmpty(dimensionYamlTpls)) {
            return;
        }
        Set<String> toAdd = dimensionYamlTpls.stream().map(m -> m.getName()).collect(Collectors.toSet());
        Iterator<Dimension> iterator = dimensions.iterator();
        while (iterator.hasNext()) {
            Dimension cur = iterator.next();
            if (toAdd.contains(cur.getName())) {
                iterator.remove();
            }
        }
        dimensions.addAll(dimensionYamlTpls);
    }

    private static void updateMetric(List<Metric> metricYamlTpls, List<Metric> metrics) {
        if (CollectionUtils.isEmpty(metricYamlTpls)) {
            return;
        }
        Set<String> toAdd = metricYamlTpls.stream().map(m -> m.getName()).collect(Collectors.toSet());
        Iterator<Metric> iterator = metrics.iterator();
        while (iterator.hasNext()) {
            Metric cur = iterator.next();
            if (toAdd.contains(cur.getName())) {
                iterator.remove();
            }
        }
        metrics.addAll(metricYamlTpls);
    }

    public static String formatKey(String key) {
        key = key.trim();
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        if (key.endsWith("/")) {
            key = key.substring(0, key.length() - 1);
        }
        return key;
    }

    @Configuration
    @EnableCaching
    public class GuavaCacheConfig {

        @Value("${parser.cache.saveMinute:15}")
        private Integer saveMinutes = 15;
        @Value("${parser.cache.maximumSize:1000}")
        private Integer maximumSize = 1000;

        @Bean
        public LoadingCache<String, SemanticModel> getCache() {
            LoadingCache<String, SemanticModel> cache
                    = CacheBuilder.newBuilder()
                    .expireAfterWrite(saveMinutes, TimeUnit.MINUTES)
                    .initialCapacity(10)
                    .maximumSize(maximumSize).build(
                            new CacheLoader<String, SemanticModel>() {
                                @Override
                                public SemanticModel load(String key) {
                                    log.info("load SemanticSchema [{}]", key);
                                    return SemanticSchemaManagerImpl.this.reload(key);
                                }
                            }
                    );
            return cache;
        }
    }


}
