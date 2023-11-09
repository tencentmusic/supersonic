package com.tencent.supersonic.semantic.model.rest;


import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.semantic.api.model.pojo.DrillDownDimension;
import com.tencent.supersonic.semantic.api.model.request.MetaBatchReq;
import com.tencent.supersonic.semantic.api.model.request.MetricReq;
import com.tencent.supersonic.semantic.api.model.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tencent.supersonic.semantic.model.domain.pojo.MetaFilter;
import com.tencent.supersonic.semantic.model.domain.pojo.MetricFilter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;


@RestController
@RequestMapping("/api/semantic/metric")
public class MetricController {


    private MetricService metricService;


    public MetricController(MetricService metricService) {
        this.metricService = metricService;
    }


    @PostMapping("/creatExprMetric")
    public Boolean creatExprMetric(@RequestBody MetricReq metricReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        metricService.createMetric(metricReq, user);
        return true;
    }

    @PostMapping("/updateExprMetric")
    public Boolean updateExprMetric(@RequestBody MetricReq metricReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        metricService.updateExprMetric(metricReq, user);
        return true;
    }

    @PostMapping("/batchUpdateStatus")
    public Boolean batchUpdateStatus(@RequestBody MetaBatchReq metaBatchReq,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        metricService.batchUpdateStatus(metaBatchReq, user);
        return true;
    }


    @PostMapping("/mockMetricAlias")
    public List<String> mockMetricAlias(@RequestBody MetricReq metricReq,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return metricService.mockAlias(metricReq, "indicator", user);
    }

    @GetMapping("/getMetricList/{modelId}")
    public List<MetricResp> getMetricList(@PathVariable("modelId") Long modelId) {
        MetaFilter metaFilter = new MetaFilter(Lists.newArrayList(modelId));
        return metricService.getMetrics(metaFilter);
    }


    @PostMapping("/queryMetric")
    public PageInfo<MetricResp> queryMetric(@RequestBody PageMetricReq pageMetricReq,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return metricService.queryMetric(pageMetricReq, user);
    }

    @Deprecated
    @GetMapping("getMetric/{modelId}/{bizName}")
    public MetricResp getMetric(@PathVariable("modelId") Long modelId, @PathVariable("bizName") String bizName) {
        return metricService.getMetric(modelId, bizName);
    }

    @GetMapping("getMetric/{id}")
    public MetricResp getMetric(@PathVariable("id") Long id,
                                HttpServletRequest request, HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return metricService.getMetric(id, user);
    }

    @DeleteMapping("deleteMetric/{id}")
    public Boolean deleteMetric(@PathVariable("id") Long id,
                                HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        metricService.deleteMetric(id, user);
        return true;
    }

    @GetMapping("/getAllHighSensitiveMetric")
    public List<MetricResp> getAllHighSensitiveMetric() {
        MetricFilter metricFilter = new MetricFilter();
        metricFilter.setSensitiveLevel(SensitiveLevelEnum.HIGH.getCode());
        return metricService.getMetrics(metricFilter);
    }


    @GetMapping("/getMetricTags")
    public Set<String> getMetricTags() {
        return metricService.getMetricTags();
    }


    @GetMapping("/getDrillDownDimension")
    public List<DrillDownDimension> getDrillDownDimension(Long metricId) {
        return metricService.getDrillDownDimension(metricId);
    }
}
