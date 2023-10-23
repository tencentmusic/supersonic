package com.tencent.supersonic.semantic.model.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.semantic.api.model.request.DatasourceRelaReq;
import com.tencent.supersonic.semantic.api.model.request.DatasourceReq;
import com.tencent.supersonic.semantic.api.model.response.DatasourceRelaResp;
import com.tencent.supersonic.semantic.api.model.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.model.response.MeasureResp;
import com.tencent.supersonic.semantic.model.domain.DatasourceService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.tencent.supersonic.semantic.model.domain.pojo.Datasource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/semantic/datasource")
public class DatasourceController {

    private DatasourceService datasourceService;

    public DatasourceController(DatasourceService datasourceService) {
        this.datasourceService = datasourceService;
    }

    @PostMapping("/createDatasource")
    public Datasource createDatasource(@RequestBody DatasourceReq datasourceReq,
                                       HttpServletRequest request,
                                       HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return datasourceService.createDatasource(datasourceReq, user);
    }

    @PostMapping("/updateDatasource")
    public Datasource updateDatasource(@RequestBody DatasourceReq datasourceReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return datasourceService.updateDatasource(datasourceReq, user);
    }

    @GetMapping("/getDatasourceList/{modelId}")
    public List<DatasourceResp> getDatasourceList(@PathVariable("modelId") Long modelId) {
        return datasourceService.getDatasourceListNoMeasurePrefix(modelId);
    }

    @GetMapping("/getMeasureListOfModel/{modelId}")
    public List<MeasureResp> getMeasureListOfModel(@PathVariable("modelId") Long modelId) {
        return datasourceService.getMeasureListOfModel(modelId);
    }

    @DeleteMapping("deleteDatasource/{id}")
    public boolean deleteDatasource(@PathVariable("id") Long id,
                                 HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        datasourceService.deleteDatasource(id, user);
        return true;
    }

    /**
     * @param datasourceRelaReq
     * @return
     */
    @PostMapping("/createOrUpdateDatasourceRela")
    public DatasourceRelaResp createOrUpdateDatasourceRela(@RequestBody DatasourceRelaReq datasourceRelaReq,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return datasourceService.createOrUpdateDatasourceRela(datasourceRelaReq, user);
    }

    @GetMapping("/getDatasourceRelaList/{modelId}")
    public List<DatasourceRelaResp> getDatasourceRelaList(@PathVariable("modelId") Long modelId) {
        return datasourceService.getDatasourceRelaList(modelId);
    }

    @DeleteMapping("/deleteDatasourceRela/{id}")
    public void deleteDatasourceRela(@PathVariable("id") Long id) {
        datasourceService.deleteDatasourceRela(id);
    }
}
