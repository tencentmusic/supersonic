package com.tencent.supersonic.semantic.core.application;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.semantic.api.core.request.ViewInfoReq;
import com.tencent.supersonic.semantic.api.core.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.core.response.DimensionResp;
import com.tencent.supersonic.semantic.api.core.response.DomainSchemaRelaResp;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.semantic.core.domain.dataobject.ViewInfoDO;
import com.tencent.supersonic.semantic.core.domain.repository.ViewInfoRepository;
import com.tencent.supersonic.semantic.core.domain.DatasourceService;
import com.tencent.supersonic.semantic.core.domain.DimensionService;
import com.tencent.supersonic.semantic.core.domain.MetricService;

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

    public List<ViewInfoDO> getViewInfoList(Long domainId) {
        return viewInfoRepository.getViewInfoList(domainId);
    }

    public List<DomainSchemaRelaResp> getDomainSchema(Long domainId) {
        List<DomainSchemaRelaResp> domainSchemaRelaResps = Lists.newArrayList();
        List<DatasourceResp> datasourceResps = datasourceService.getDatasourceList(domainId);
        for (DatasourceResp datasourceResp : datasourceResps) {
            DomainSchemaRelaResp domainSchemaRelaResp = new DomainSchemaRelaResp();
            Long datasourceId = datasourceResp.getId();
            List<MetricResp> metricResps = metricService.getMetrics(domainId, datasourceId);
            List<DimensionResp> dimensionResps = dimensionService.getDimensionsByDatasource(datasourceId);
            domainSchemaRelaResp.setDatasource(datasourceResp);
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
