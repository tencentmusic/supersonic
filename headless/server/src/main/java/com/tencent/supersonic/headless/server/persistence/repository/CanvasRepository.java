package com.tencent.supersonic.headless.server.persistence.repository;

import com.tencent.supersonic.headless.server.persistence.dataobject.CanvasDO;

import java.util.List;

public interface CanvasRepository {

    List<CanvasDO> getCanvasList(Long domainId);

    CanvasDO getCanvasById(Long id);

    void deleteCanvas(Long id);

    void createCanvas(CanvasDO canvasDO);

    void updateCanvas(CanvasDO canvasDO);
}
