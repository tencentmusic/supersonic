package com.tencent.supersonic.semantic.core.domain;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.core.pojo.ItemDateFilter;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.DatasourceYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.semantic.api.core.pojo.yaml.MetricYamlTpl;
import com.tencent.supersonic.semantic.api.core.request.DatasourceRelaReq;
import com.tencent.supersonic.semantic.api.core.request.DatasourceReq;
import com.tencent.supersonic.semantic.api.core.response.DatasourceRelaResp;
import com.tencent.supersonic.semantic.api.core.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.core.response.ItemDateResp;
import com.tencent.supersonic.semantic.api.core.response.MeasureResp;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DatasourceService {

    DatasourceResp createDatasource(DatasourceReq datasourceReq, User user) throws Exception;

    DatasourceResp updateDatasource(DatasourceReq datasourceReq, User user) throws Exception;

    String getSourceBizNameById(Long id);

    List<DatasourceResp> getDatasourceListNoMeasurePrefix(Long domainId);

    List<DatasourceResp> getDatasourceList();

    List<DatasourceResp> getDatasourceList(Long domainId);

    Map<Long, DatasourceResp> getDatasourceMap();

    void deleteDatasource(Long id) throws Exception;

    DatasourceRelaResp createOrUpdateDatasourceRela(DatasourceRelaReq datasourceRelaReq, User user);

    List<DatasourceRelaResp> getDatasourceRelaList(Long domainId);

    void deleteDatasourceRela(Long id);

    ItemDateResp getDateDate(ItemDateFilter dimension, ItemDateFilter metric);

    List<MeasureResp> getMeasureListOfDomain(Long domainId);


    void getModelYamlTplByDomainIds(Set<Long> domainIds, Map<String, List<DimensionYamlTpl>> dimensionYamlMap,
            List<DatasourceYamlTpl> datasourceYamlTplList, List<MetricYamlTpl> metricYamlTplList);

}
