package com.tencent.supersonic.semantic.model.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.enums.AuthType;
import com.tencent.supersonic.semantic.api.model.request.ModelReq;
import com.tencent.supersonic.semantic.api.model.response.DatabaseResp;
import com.tencent.supersonic.semantic.api.model.response.ModelResp;
import com.tencent.supersonic.semantic.model.domain.ModelService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/semantic/model")
public class ModelController {

    private ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @PostMapping("/createModel")
    public Boolean createModel(@RequestBody ModelReq modelReq,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        modelService.createModel(modelReq, user);
        return true;
    }

    @PostMapping("/updateModel")
    public Boolean updateModel(@RequestBody ModelReq modelReq,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        modelService.updateModel(modelReq, user);
        return true;
    }

    @DeleteMapping("/deleteModel/{modelId}")
    public Boolean deleteModel(@PathVariable("modelId") Long modelId,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        modelService.deleteModel(modelId, user);
        return true;
    }

    @GetMapping("/getModelList/{domainId}")
    public List<ModelResp> getModelList(@PathVariable("domainId") Long domainId,
                                         HttpServletRequest request,
                                          HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return modelService.getModelListWithAuth(user, domainId, AuthType.ADMIN);
    }


    @GetMapping("/getModel/{id}")
    public ModelResp getModel(@PathVariable("id") Long id) {
        return modelService.getModel(id);
    }

    @GetMapping("/getModelListByIds/{modelIds}")
    public List<ModelResp> getModelListByIds(@PathVariable("modelIds") String modelIds) {
        return modelService.getModelList(Arrays.stream(modelIds.split(",")).map(Long::parseLong)
                .collect(Collectors.toList()));
    }

    @GetMapping("/getModelDatabase/{modelId}")
    public DatabaseResp getModelDatabase(@PathVariable("modelId") Long modelId) {
        return modelService.getDatabaseByModelId(modelId);
    }

}
