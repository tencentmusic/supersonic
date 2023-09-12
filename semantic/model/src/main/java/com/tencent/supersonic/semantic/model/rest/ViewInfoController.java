package com.tencent.supersonic.semantic.model.rest;


import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.semantic.api.model.request.ViewInfoReq;
import com.tencent.supersonic.semantic.api.model.response.ModelSchemaRelaResp;
import com.tencent.supersonic.semantic.model.domain.dataobject.ViewInfoDO;
import com.tencent.supersonic.semantic.model.application.ViewInfoServiceImpl;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/getViewInfoList/{modelId}")
    public List<ViewInfoDO> getViewInfoList(@PathVariable("modelId") Long modelId) {
        return viewInfoServiceImpl.getViewInfoList(modelId);
    }

    @DeleteMapping("/deleteViewInfo/{id}")
    public void deleteViewInfo(@PathVariable("id") Long id) {
        viewInfoServiceImpl.deleteViewInfo(id);
    }

    @GetMapping("/getDomainSchemaRela/{modelId}")
    public List<ModelSchemaRelaResp> getDomainSchema(@PathVariable("modelId") Long modelId) {
        return viewInfoServiceImpl.getDomainSchema(modelId);
    }


}
