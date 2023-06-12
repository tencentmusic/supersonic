package com.tencent.supersonic.semantic.core.infrastructure.repository;

import com.tencent.supersonic.semantic.core.domain.dataobject.ViewInfoDO;
import com.tencent.supersonic.semantic.core.domain.dataobject.ViewInfoDOExample;
import com.tencent.supersonic.semantic.core.domain.repository.ViewInfoRepository;
import com.tencent.supersonic.semantic.core.infrastructure.mapper.ViewInfoDOMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ViewInfoRepositoryImpl implements ViewInfoRepository {


    private ViewInfoDOMapper viewInfoDOMapper;

    public ViewInfoRepositoryImpl(ViewInfoDOMapper viewInfoDOMapper) {
        this.viewInfoDOMapper = viewInfoDOMapper;
    }

    @Override
    public List<ViewInfoDO> getViewInfoList(Long domainId) {
        ViewInfoDOExample viewInfoDOExample = new ViewInfoDOExample();
        viewInfoDOExample.createCriteria().andDomainIdEqualTo(domainId);
        return viewInfoDOMapper.selectByExampleWithBLOBs(viewInfoDOExample);
    }


    @Override
    public ViewInfoDO getViewInfoById(Long id) {
        return viewInfoDOMapper.selectByPrimaryKey(id);
    }


    @Override
    public void deleteViewInfo(Long id) {
        viewInfoDOMapper.deleteByPrimaryKey(id);
    }


    @Override
    public void createViewInfo(ViewInfoDO viewInfoDO) {
        viewInfoDOMapper.insert(viewInfoDO);
    }


    @Override
    public void updateViewInfo(ViewInfoDO viewInfoDO) {
        viewInfoDOMapper.updateByPrimaryKeyWithBLOBs(viewInfoDO);
    }


}
