package com.tencent.supersonic.semantic.model.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.semantic.model.domain.dataobject.ViewInfoDO;
import com.tencent.supersonic.semantic.model.domain.repository.ViewInfoRepository;
import com.tencent.supersonic.semantic.model.infrastructure.mapper.ViewInfoDOMapper;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ViewInfoRepositoryImpl implements ViewInfoRepository {


    private ViewInfoDOMapper viewInfoDOMapper;

    public ViewInfoRepositoryImpl(ViewInfoDOMapper viewInfoDOMapper) {
        this.viewInfoDOMapper = viewInfoDOMapper;
    }

    @Override
    public List<ViewInfoDO> getViewInfoList(Long domainId) {
        QueryWrapper<ViewInfoDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ViewInfoDO::getDomainId, domainId);
        return viewInfoDOMapper.selectList(wrapper);
    }

    @Override
    public ViewInfoDO getViewInfoById(Long id) {
        return viewInfoDOMapper.selectById(id);
    }

    @Override
    public void deleteViewInfo(Long id) {
        viewInfoDOMapper.deleteById(id);
    }

    @Override
    public void createViewInfo(ViewInfoDO viewInfoDO) {
        viewInfoDOMapper.insert(viewInfoDO);
    }

    @Override
    public void updateViewInfo(ViewInfoDO viewInfoDO) {
        viewInfoDOMapper.updateById(viewInfoDO);
    }

}
