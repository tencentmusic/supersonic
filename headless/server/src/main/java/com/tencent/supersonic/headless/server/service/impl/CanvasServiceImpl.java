package com.tencent.supersonic.headless.server.service.impl;


import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.api.pojo.request.CanvasReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.api.pojo.response.CanvasSchemaResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.CanvasDO;
import com.tencent.supersonic.headless.server.persistence.repository.CanvasRepository;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CanvasServiceImpl {

    private CanvasRepository canvasRepository;

    private ModelService modelService;

    private DimensionService dimensionService;

    private MetricService metricService;

    public CanvasServiceImpl(CanvasRepository canvasRepository, ModelService modelService,
                             MetricService metricService, DimensionService dimensionService) {
        this.canvasRepository = canvasRepository;
        this.dimensionService = dimensionService;
        this.metricService = metricService;
        this.modelService = modelService;
    }

    public List<CanvasDO> getCanvasList(Long domainId) {
        return canvasRepository.getCanvasList(domainId);
    }

    public List<CanvasSchemaResp> getCanvasSchema(Long domainId, User user) {
        List<CanvasSchemaResp> canvasSchemaResps = Lists.newArrayList();
        List<ModelResp> modelResps = modelService.getModelListWithAuth(user, domainId, AuthType.ADMIN);
        for (ModelResp modelResp : modelResps) {
            CanvasSchemaResp canvasSchemaResp = new CanvasSchemaResp();
            MetaFilter metaFilter = new MetaFilter();
            metaFilter.setModelIds(Lists.newArrayList(modelResp.getId()));
            List<MetricResp> metricResps = metricService.getMetrics(metaFilter);
            List<DimensionResp> dimensionResps = dimensionService.getDimensions(metaFilter);
            canvasSchemaResp.setModel(modelResp);
            canvasSchemaResp.setDimensions(dimensionResps);
            canvasSchemaResp.setMetrics(metricResps);
            canvasSchemaResp.setDomainId(domainId);
            canvasSchemaResps.add(canvasSchemaResp);
        }
        return canvasSchemaResps;
    }

    public CanvasDO createOrUpdateCanvas(CanvasReq canvasReq, User user) {
        if (canvasReq.getId() == null) {
            CanvasDO viewInfoDO = new CanvasDO();
            BeanUtils.copyProperties(canvasReq, viewInfoDO);
            viewInfoDO.setCreatedAt(new Date());
            viewInfoDO.setCreatedBy(user.getName());
            viewInfoDO.setUpdatedAt(new Date());
            viewInfoDO.setUpdatedBy(user.getName());
            canvasRepository.createCanvas(viewInfoDO);
            return viewInfoDO;
        }
        Long id = canvasReq.getId();
        CanvasDO viewInfoDO = canvasRepository.getCanvasById(id);
        BeanUtils.copyProperties(canvasReq, viewInfoDO);
        viewInfoDO.setUpdatedAt(new Date());
        viewInfoDO.setUpdatedBy(user.getName());
        canvasRepository.updateCanvas(viewInfoDO);
        return viewInfoDO;
    }

    public void deleteCanvas(Long id) {
        canvasRepository.deleteCanvas(id);
    }

}
