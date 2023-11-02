package com.tencent.supersonic.semantic.model.application;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.model.request.ViewInfoReq;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaRelaResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.model.domain.dataobject.ViewInfoDO;
import com.tencent.supersonic.semantic.model.domain.pojo.MetaFilter;
import com.tencent.supersonic.semantic.model.domain.repository.ViewInfoRepository;
import com.tencent.supersonic.semantic.model.domain.DatasourceService;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.MetricService;

import java.util.Date;
import java.util.List;
import org.assertj.core.util.Lists;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class ViewInfoServiceImpl {

    private ViewInfoRepository viewInfoRepository;

    private DatasourceService datasourceService;

    private DimensionService dimensionService;

    private MetricService metricService;

    public ViewInfoServiceImpl(ViewInfoRepository viewInfoRepository, DatasourceService datasourceService,
            MetricService metricService, DimensionService dimensionService) {
        this.viewInfoRepository = viewInfoRepository;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
        this.datasourceService = datasourceService;
    }

    public List<ViewInfoDO> getViewInfoList(Long modelId) {
        return viewInfoRepository.getViewInfoList(modelId);
    }

    public List<ModelSchemaRelaResp> getDomainSchema(Long modelId) {
        List<ModelSchemaRelaResp> domainSchemaRelaResps = Lists.newArrayList();
        List<DatasourceResp> datasourceResps = datasourceService.getDatasourceList(modelId);
        for (DatasourceResp datasourceResp : datasourceResps) {
            ModelSchemaRelaResp domainSchemaRelaResp = new ModelSchemaRelaResp();
            Long datasourceId = datasourceResp.getId();
            MetaFilter metaFilter = new MetaFilter();
            metaFilter.setModelIds(Lists.newArrayList(modelId));
            metaFilter.setDatasourceId(datasourceId);
            List<MetricResp> metricResps = metricService.getMetrics(metaFilter);
            List<DimensionResp> dimensionResps = dimensionService.getDimensions(metaFilter);
            domainSchemaRelaResp.setDatasource(datasourceResp);
            domainSchemaRelaResp.setDimensions(dimensionResps);
            domainSchemaRelaResp.setMetrics(metricResps);
            domainSchemaRelaResp.setDomainId(modelId);
            domainSchemaRelaResps.add(domainSchemaRelaResp);
        }
        return domainSchemaRelaResps;
    }


    public ViewInfoDO createOrUpdateViewInfo(ViewInfoReq viewInfoReq, User user) {
        if (viewInfoReq.getId() == null) {
            ViewInfoDO viewInfoDO = new ViewInfoDO();
            BeanUtils.copyProperties(viewInfoReq, viewInfoDO);
            viewInfoDO.setCreatedAt(new Date());
            viewInfoDO.setCreatedBy(user.getName());
            viewInfoDO.setUpdatedAt(new Date());
            viewInfoDO.setUpdatedBy(user.getName());
            viewInfoRepository.createViewInfo(viewInfoDO);
            return viewInfoDO;
        }
        Long id = viewInfoReq.getId();
        ViewInfoDO viewInfoDO = viewInfoRepository.getViewInfoById(id);
        BeanUtils.copyProperties(viewInfoReq, viewInfoDO);
        viewInfoDO.setUpdatedAt(new Date());
        viewInfoDO.setUpdatedBy(user.getName());
        viewInfoRepository.updateViewInfo(viewInfoDO);
        return viewInfoDO;
    }


    public void deleteViewInfo(Long id) {
        viewInfoRepository.deleteViewInfo(id);
    }

}
