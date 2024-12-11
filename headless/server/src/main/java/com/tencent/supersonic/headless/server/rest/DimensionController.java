package com.tencent.supersonic.headless.server.rest;

import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.headless.api.pojo.DimValueMap;
import com.tencent.supersonic.headless.api.pojo.MetaFilter;
import com.tencent.supersonic.headless.api.pojo.request.DimValueAliasReq;
import com.tencent.supersonic.headless.api.pojo.request.DimensionReq;
import com.tencent.supersonic.headless.api.pojo.request.DimensionValueReq;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.PageDimensionReq;
import com.tencent.supersonic.headless.api.pojo.response.DimensionResp;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.pojo.DimensionFilter;
import com.tencent.supersonic.headless.server.service.DimensionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/semantic/dimension")
public class DimensionController {

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private SemanticLayerService queryService;

    /**
     * 创建维度
     *
     * @param dimensionReq
     */
    @PostMapping("/createDimension")
    public DimensionResp createDimension(@RequestBody DimensionReq dimensionReq,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return dimensionService.createDimension(dimensionReq, user);
    }

    @PostMapping("/updateDimension")
    public Boolean updateDimension(@RequestBody DimensionReq dimensionReq,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        dimensionService.updateDimension(dimensionReq, user);
        return true;
    }

    @PostMapping("/updateDimension/alias/value")
    public Boolean updateDimValueAlias(@RequestBody DimValueAliasReq req,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        dimensionService.updateDimValueAlias(req, user);
        return true;
    }

    @PostMapping("/batchUpdateStatus")
    public Boolean batchUpdateStatus(@RequestBody MetaBatchReq metaBatchReq,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        dimensionService.batchUpdateStatus(metaBatchReq, user);
        return true;
    }

    @PostMapping("/batchUpdateSensitiveLevel")
    public Boolean batchUpdateSensitiveLevel(@RequestBody MetaBatchReq metaBatchReq,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        dimensionService.batchUpdateSensitiveLevel(metaBatchReq, user);
        return true;
    }

    @PostMapping("/mockDimensionAlias")
    public List<String> mockMetricAlias(@RequestBody DimensionReq dimensionReq,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return dimensionService.mockAlias(dimensionReq, "dimension", user);
    }

    @PostMapping("/mockDimensionValuesAlias")
    public List<DimValueMap> mockDimensionValuesAlias(@RequestBody DimensionReq dimensionReq,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return dimensionService.mockDimensionValueAlias(dimensionReq, user);
    }

    @GetMapping("/getDimensionList/{modelId}")
    public List<DimensionResp> getDimension(@PathVariable("modelId") Long modelId) {
        DimensionFilter dimensionFilter = new DimensionFilter();
        dimensionFilter.setModelIds(Lists.newArrayList(modelId));
        return dimensionService.getDimensions(dimensionFilter);
    }

    @GetMapping("/getDimensionInModelCluster/{modelId}")
    public List<DimensionResp> getDimensionInModelCluster(@PathVariable("modelId") Long modelId) {
        return dimensionService.getDimensionInModelCluster(modelId);
    }

    @GetMapping("/{modelId}/{dimensionName}")
    public DimensionResp getDimensionDescByNameAndId(@PathVariable("modelId") Long modelId,
            @PathVariable("dimensionName") String dimensionBizName) {
        return dimensionService.getDimension(dimensionBizName, modelId);
    }

    @PostMapping("/queryDimension")
    public PageInfo<DimensionResp> queryDimension(@RequestBody PageDimensionReq pageDimensionReq) {
        return dimensionService.queryDimension(pageDimensionReq);
    }

    @PostMapping("/queryDimValue")
    public SemanticQueryResp queryDimValue(@RequestBody DimensionValueReq dimensionValueReq,
            HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return queryService.queryDimensionValue(dimensionValueReq, user);
    }

    @DeleteMapping("deleteDimension/{id}")
    public Boolean deleteDimension(@PathVariable("id") Long id, HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        dimensionService.deleteDimension(id, user);
        return true;
    }

    @GetMapping("/getAllHighSensitiveDimension")
    public List<DimensionResp> getAllHighSensitiveDimension() {
        MetaFilter metaFilter = new MetaFilter();
        metaFilter.setSensitiveLevel(SensitiveLevelEnum.HIGH.getCode());
        return dimensionService.getDimensions(metaFilter);
    }
}
