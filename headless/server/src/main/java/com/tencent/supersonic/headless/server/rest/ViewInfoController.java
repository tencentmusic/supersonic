package com.tencent.supersonic.headless.server.rest;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.common.server.request.ViewInfoReq;
import com.tencent.supersonic.headless.common.server.response.ModelSchemaRelaResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.ViewInfoDO;
import com.tencent.supersonic.headless.server.service.impl.ViewInfoServiceImpl;
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
public class ViewInfoController {

    private ViewInfoServiceImpl viewInfoServiceImpl;

    public ViewInfoController(ViewInfoServiceImpl viewInfoServiceImpl) {
        this.viewInfoServiceImpl = viewInfoServiceImpl;
    }

    @PostMapping("/createOrUpdateViewInfo")
    public ViewInfoDO createOrUpdateViewInfo(@RequestBody ViewInfoReq viewInfoReq, HttpServletRequest request,
                                             HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return viewInfoServiceImpl.createOrUpdateViewInfo(viewInfoReq, user);
    }

    @GetMapping("/getViewInfoList/{domainId}")
    public List<ViewInfoDO> getViewInfoList(@PathVariable("domainId") Long domainId) {
        return viewInfoServiceImpl.getViewInfoList(domainId);
    }

    @DeleteMapping("/deleteViewInfo/{id}")
    public void deleteViewInfo(@PathVariable("id") Long id) {
        viewInfoServiceImpl.deleteViewInfo(id);
    }

    @GetMapping("/getDomainSchemaRela/{domainId}")
    public List<ModelSchemaRelaResp> getDomainSchema(@PathVariable("domainId") Long domainId,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return viewInfoServiceImpl.getDomainSchema(domainId, user);
    }

}
