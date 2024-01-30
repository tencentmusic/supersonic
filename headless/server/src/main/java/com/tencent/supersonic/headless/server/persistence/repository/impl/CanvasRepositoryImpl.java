package com.tencent.supersonic.headless.server.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tencent.supersonic.headless.server.persistence.dataobject.CanvasDO;
import com.tencent.supersonic.headless.server.persistence.mapper.CanvasDOMapper;
import com.tencent.supersonic.headless.server.persistence.repository.CanvasRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CanvasRepositoryImpl implements CanvasRepository {


    private CanvasDOMapper canvasDOMapper;

    public CanvasRepositoryImpl(CanvasDOMapper canvasDOMapper) {
        this.canvasDOMapper = canvasDOMapper;
    }

    @Override
    public List<CanvasDO> getCanvasList(Long domainId) {
        QueryWrapper<CanvasDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(CanvasDO::getDomainId, domainId);
        return canvasDOMapper.selectList(wrapper);
    }

    @Override
    public CanvasDO getCanvasById(Long id) {
        return canvasDOMapper.selectById(id);
    }

    @Override
    public void deleteCanvas(Long id) {
        canvasDOMapper.deleteById(id);
    }

    @Override
    public void createCanvas(CanvasDO canvasDO) {
        canvasDOMapper.insert(canvasDO);
    }

    @Override
    public void updateCanvas(CanvasDO canvasDO) {
        canvasDOMapper.updateById(canvasDO);
    }

}
