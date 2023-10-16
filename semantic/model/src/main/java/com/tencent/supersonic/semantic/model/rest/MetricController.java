package com.tencent.supersonic.semantic.model.rest;


import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.semantic.api.model.pojo.DrillDownDimension;
import com.tencent.supersonic.semantic.api.model.request.MetricReq;
import com.tencent.supersonic.semantic.api.model.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.model.response.MetricResp;
import com.tencent.supersonic.semantic.model.domain.MetricService;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
        metricService.creatExprMetric(metricReq, user);
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


    @PostMapping("/mockMetricAlias")
    public List<String> mockMetricAlias(@RequestBody MetricReq metricReq,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return metricService.mockAlias(metricReq, "indicator", user);
    }

    @GetMapping("/getMetricList/{modelId}")
    public List<MetricResp> getMetricList(@PathVariable("modelId") Long modelId) {
        return metricService.getMetrics(modelId);
    }


    @PostMapping("/queryMetric")
    public PageInfo<MetricResp> queryMetric(@RequestBody PageMetricReq pageMetricReq,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return metricService.queryMetric(pageMetricReq, user);
    }

    @GetMapping("getMetric/{modelId}/{bizName}")
    public MetricResp getMetric(@PathVariable("modelId") Long modelId, @PathVariable("bizName") String bizName) {
        return metricService.getMetric(modelId, bizName);
    }


    @DeleteMapping("deleteMetric/{id}")
    public Boolean deleteMetric(@PathVariable("id") Long id) throws Exception {
        metricService.deleteMetric(id);
        return true;
    }

    @GetMapping("/getAllHighSensitiveMetric")
    public List<MetricResp> getAllHighSensitiveMetric() {
        return metricService.getAllHighSensitiveMetric();
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
