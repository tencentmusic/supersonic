package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.request.CanvasReq;
import com.tencent.supersonic.headless.api.pojo.response.CanvasSchemaResp;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.api.pojo.response.ModelResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.CanvasDO;
import com.tencent.supersonic.headless.server.persistence.mapper.CanvasDOMapper;
import com.tencent.supersonic.headless.server.service.CanvasService;
import com.tencent.supersonic.headless.server.service.DimensionService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.ModelService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CanvasServiceImpl extends ServiceImpl<CanvasDOMapper, CanvasDO>
        implements CanvasService {

    @Autowired
    private ModelService modelService;

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private MetricService metricService;

    @Override
    public List<CanvasDO> getCanvasList(Long domainId) {
        QueryWrapper<CanvasDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(CanvasDO::getDomainId, domainId);
        return list(queryWrapper);
    }

    @Override
    public List<CanvasSchemaResp> getCanvasSchema(Long domainId, User user) {
        List<CanvasSchemaResp> canvasSchemaResps = Lists.newArrayList();
        List<ModelResp> modelResps =
                modelService.getModelListWithAuth(user, domainId, AuthType.ADMIN);
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

    @Override
    public CanvasDO createOrUpdateCanvas(CanvasReq canvasReq, User user) {
        if (canvasReq.getId() == null) {
            canvasReq.createdBy(user.getName());
            CanvasDO viewInfoDO = new CanvasDO();
            BeanUtils.copyProperties(canvasReq, viewInfoDO);
            save(viewInfoDO);
            return viewInfoDO;
        }
        Long id = canvasReq.getId();
        CanvasDO viewInfoDO = getById(id);
        canvasReq.updatedBy(user.getName());
        BeanUtils.copyProperties(canvasReq, viewInfoDO);
        updateById(viewInfoDO);
        return viewInfoDO;
    }

    @Override
    public void deleteCanvas(Long id) {
        removeById(id);
    }
}
