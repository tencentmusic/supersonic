package com.tencent.supersonic.headless.server.web.rest;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.api.pojo.request.CanvasReq;
import com.tencent.supersonic.headless.api.pojo.response.CanvasSchemaResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.CanvasDO;
import com.tencent.supersonic.headless.server.web.service.CanvasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/semantic/viewInfo")
public class CanvasController {

    @Autowired
    private CanvasService canvasService;

    @PostMapping("/createOrUpdateViewInfo")
    public CanvasDO createOrUpdateCanvas(@RequestBody CanvasReq canvasReq, HttpServletRequest request,
                                           HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return canvasService.createOrUpdateCanvas(canvasReq, user);
    }

    @GetMapping("/getViewInfoList/{domainId}")
    public List<CanvasDO> getCanvasList(@PathVariable("domainId") Long domainId) {
        return canvasService.getCanvasList(domainId);
    }

    @DeleteMapping("/deleteViewInfo/{id}")
    public void deleteCanvas(@PathVariable("id") Long id) {
        canvasService.deleteCanvas(id);
    }

    @GetMapping("/getDomainSchemaRela/{domainId}")
    public List<CanvasSchemaResp> getDomainSchema(@PathVariable("domainId") Long domainId,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return canvasService.getCanvasSchema(domainId, user);
    }

}
