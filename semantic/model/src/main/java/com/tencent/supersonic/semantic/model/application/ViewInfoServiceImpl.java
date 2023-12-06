package com.tencent.supersonic.semantic.model.application;


import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.model.request.ViewInfoReq;
import com.tencent.supersonic.semantic.api.model.response.DimensionResp;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaRelaResp;
import com.tencent.supersonic.semantic.model.domain.DimensionService;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import com.tencent.supersonic.semantic.model.domain.dataobject.ViewInfoDO;
import com.tencent.supersonic.semantic.model.domain.pojo.MetaFilter;
import com.tencent.supersonic.semantic.model.domain.pojo.ModelFilter;
import com.tencent.supersonic.semantic.model.domain.repository.ViewInfoRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ViewInfoServiceImpl {

    private ViewInfoRepository viewInfoRepository;

    private ModelService modelService;

    private DimensionService dimensionService;

    private MetricService metricService;

    public ViewInfoServiceImpl(ViewInfoRepository viewInfoRepository, ModelService modelService,
            MetricService metricService, DimensionService dimensionService) {
        this.viewInfoRepository = viewInfoRepository;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
        this.modelService = modelService;
    }

    public List<ViewInfoDO> getViewInfoList(Long domainId) {
        return viewInfoRepository.getViewInfoList(domainId);
    }

    public List<ModelSchemaRelaResp> getDomainSchema(Long domainId) {
        List<ModelSchemaRelaResp> domainSchemaRelaResps = Lists.newArrayList();
        ModelFilter modelFilter = new ModelFilter();
        modelFilter.setDomainIds(Lists.newArrayList(domainId));
        List<ModelResp> modelResps = modelService.getModelList(modelFilter);
        for (ModelResp modelResp : modelResps) {
            ModelSchemaRelaResp domainSchemaRelaResp = new ModelSchemaRelaResp();
            MetaFilter metaFilter = new MetaFilter();
            metaFilter.setModelIds(Lists.newArrayList(modelResp.getId()));
            List<MetricResp> metricResps = metricService.getMetrics(metaFilter);
            List<DimensionResp> dimensionResps = dimensionService.getDimensions(metaFilter);
            domainSchemaRelaResp.setModel(modelResp);
            domainSchemaRelaResp.setDimensions(dimensionResps);
            domainSchemaRelaResp.setMetrics(metricResps);
            domainSchemaRelaResp.setDomainId(domainId);
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
