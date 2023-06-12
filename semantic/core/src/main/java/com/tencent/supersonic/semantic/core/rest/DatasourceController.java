package com.tencent.supersonic.semantic.core.rest;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.semantic.api.core.request.DatasourceRelaReq;
import com.tencent.supersonic.semantic.api.core.request.DatasourceReq;
import com.tencent.supersonic.semantic.api.core.response.DatasourceRelaResp;
import com.tencent.supersonic.semantic.api.core.response.DatasourceResp;
import com.tencent.supersonic.semantic.api.core.response.MeasureResp;
import com.tencent.supersonic.semantic.core.domain.DatasourceService;
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
@RequestMapping("/api/semantic/datasource")
public class DatasourceController {


    private DatasourceService datasourceService;


    public DatasourceController(DatasourceService datasourceService) {
        this.datasourceService = datasourceService;
    }

    @PostMapping("/createDatasource")
    public DatasourceResp createDatasource(@RequestBody DatasourceReq datasourceReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return datasourceService.createDatasource(datasourceReq, user);
    }


    @PostMapping("/updateDatasource")
    public DatasourceResp updateDatasource(@RequestBody DatasourceReq datasourceReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return datasourceService.updateDatasource(datasourceReq, user);
    }

    @GetMapping("/getDatasourceList/{domainId}")
    public List<DatasourceResp> getDatasourceList(@PathVariable("domainId") Long domainId) {
        return datasourceService.getDatasourceListNoMeasurePrefix(domainId);
    }

    @GetMapping("/getMeasureListOfDomain/{domainId}")
    public List<MeasureResp> getMeasureListOfDomain(@PathVariable("domainId") Long domainId) {
        return datasourceService.getMeasureListOfDomain(domainId);
    }


    @DeleteMapping("deleteDatasource/{id}")
    public void deleteDatasource(@PathVariable("id") Long id) throws Exception {
        datasourceService.deleteDatasource(id);
    }

    /**
     * @param datasourceRelaReq
     * @return
     */
    @PostMapping("/createOrUpdateDatasourceRela")
    public DatasourceRelaResp createOrUpdateDatasourceRela(@RequestBody DatasourceRelaReq datasourceRelaReq,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return datasourceService.createOrUpdateDatasourceRela(datasourceRelaReq, user);
    }

    @GetMapping("/getDatasourceRelaList/{domainId}")
    public List<DatasourceRelaResp> getDatasourceRelaList(@PathVariable("domainId") Long domainId) {
        return datasourceService.getDatasourceRelaList(domainId);
    }

    @DeleteMapping("/deleteDatasourceRela/{id}")
    public void deleteDatasourceRela(@PathVariable("id") Long id) {
        datasourceService.deleteDatasourceRela(id);
    }
}
