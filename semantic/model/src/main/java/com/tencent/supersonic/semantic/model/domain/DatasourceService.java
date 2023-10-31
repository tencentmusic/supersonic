package com.tencent.supersonic.semantic.model.domain;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.model.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.model.yaml.DatasourceYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.model.yaml.MetricYamlTpl;
import com.tencent.supersonic.semantic.api.model.request.DatasourceRelaReq;
import com.tencent.supersonic.semantic.api.model.request.DatasourceReq;
import com.tencent.supersonic.semantic.api.model.response.DatasourceRelaResp;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.ItemDateResp;
import com.tencent.supersonic.semantic.api.model.response.MeasureResp;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DatasourceService {

    DatasourceResp createDatasource(DatasourceReq datasourceReq, User user) throws Exception;

    DatasourceResp updateDatasource(DatasourceReq datasourceReq, User user) throws Exception;

    List<DatasourceResp> getDatasourceListNoMeasurePrefix(Long modelId);

    List<DatasourceResp> getDatasourceList();

    List<DatasourceResp> getDatasourceList(Long modelId);

    List<DatasourceResp> getDatasourceByDatabase(Long databaseId);

    Map<Long, DatasourceResp> getDatasourceMap();

    void deleteDatasource(Long id) throws Exception;

    DatasourceRelaResp createOrUpdateDatasourceRela(DatasourceRelaReq datasourceRelaReq, User user);

    List<DatasourceRelaResp> getDatasourceRelaList(Long modelId);

    void deleteDatasourceRela(Long id);

    ItemDateResp getItemDate(ItemDateFilter dimension, ItemDateFilter metric);

    List<MeasureResp> getMeasureListOfModel(Long modelId);

    List<MeasureResp> getMeasureListOfModel(List<Long> modelIds);

    void getModelYamlTplByModelIds(Set<Long> modelIds, Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DatasourceYamlTpl> datasourceYamlTplList, List<MetricYamlTpl> metricYamlTplList);

}
