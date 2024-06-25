package com.tencent.supersonic.headless.server.web.service;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.CanvasReq;
import com.tencent.supersonic.headless.api.pojo.response.CanvasSchemaResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.CanvasDO;

import java.util.List;

public interface CanvasService {

    List<CanvasDO> getCanvasList(Long domainId);

    List<CanvasSchemaResp> getCanvasSchema(Long domainId, User user);

    CanvasDO createOrUpdateCanvas(CanvasReq canvasReq, User user);

    void deleteCanvas(Long id);
}
