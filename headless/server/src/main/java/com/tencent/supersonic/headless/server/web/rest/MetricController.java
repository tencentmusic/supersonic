package com.tencent.supersonic.headless.server.web.rest;


import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.enums.SensitiveLevelEnum;
import com.tencent.supersonic.headless.api.pojo.DrillDownDimension;
import com.tencent.supersonic.headless.api.pojo.MetricQueryDefaultConfig;
import com.tencent.supersonic.headless.api.pojo.request.MetaBatchReq;
import com.tencent.supersonic.headless.api.pojo.request.MetricBaseReq;
import com.tencent.supersonic.headless.api.pojo.request.MetricReq;
import com.tencent.supersonic.headless.api.pojo.request.PageMetricReq;
import com.tencent.supersonic.headless.api.pojo.response.MetricResp;
import com.tencent.supersonic.headless.server.pojo.MetaFilter;
import com.tencent.supersonic.headless.server.pojo.MetricFilter;
import com.tencent.supersonic.headless.server.web.service.MetricService;
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
import java.util.Set;


@RestController
@RequestMapping("/api/semantic/metric")
public class MetricController {

    private MetricService metricService;

    public MetricController(MetricService metricService) {
        this.metricService = metricService;
    }

    @PostMapping("/createMetric")
    public MetricResp createMetric(@RequestBody MetricReq metricReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return metricService.createMetric(metricReq, user);
    }

    @PostMapping("/updateMetric")
    public MetricResp updateMetric(@RequestBody MetricReq metricReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        return metricService.updateMetric(metricReq, user);
    }

    @PostMapping("/batchUpdateStatus")
    public Boolean batchUpdateStatus(@RequestBody MetaBatchReq metaBatchReq,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        metricService.batchUpdateStatus(metaBatchReq, user);
        return true;
    }

    @PostMapping("/batchPublish")
    public Boolean batchPublish(@RequestBody MetaBatchReq metaBatchReq,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        metricService.batchPublish(metaBatchReq.getIds(), user);
        return true;
    }

    @PostMapping("/batchUnPublish")
    public Boolean batchUnPublish(@RequestBody MetaBatchReq metaBatchReq,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        metricService.batchUnPublish(metaBatchReq.getIds(), user);
        return true;
    }

    @PostMapping("/batchUpdateClassifications")
    public Boolean batchUpdateClassifications(@RequestBody MetaBatchReq metaBatchReq,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        metricService.batchUpdateClassifications(metaBatchReq, user);
        return true;
    }

    @PostMapping("/batchUpdateSensitiveLevel")
    public Boolean batchUpdateSensitiveLevel(@RequestBody MetaBatchReq metaBatchReq,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        metricService.batchUpdateSensitiveLevel(metaBatchReq, user);
        return true;
    }

    @PostMapping("/mockMetricAlias")
    public List<String> mockMetricAlias(@RequestBody MetricBaseReq metricReq,
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

    @GetMapping("/getMetricsToCreateNewMetric/{modelId}")
    public List<MetricResp> getMetricsToCreateNewMetric(@PathVariable("modelId") Long modelId) {
        return metricService.getMetricsToCreateNewMetric(modelId);
    }

    @PostMapping("/queryMetric")
    public PageInfo<MetricResp> queryMetric(@RequestBody PageMetricReq pageMetricReq,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return metricService.queryMetricMarket(pageMetricReq, user);
    }

    @Deprecated
    @GetMapping("getMetric/{modelId}/{bizName}")
    public MetricResp getMetric(@PathVariable("modelId") Long modelId,
            @PathVariable("bizName") String bizName) {
        return metricService.getMetric(modelId, bizName);
    }

    @GetMapping("getMetric/{id}")
    public MetricResp getMetric(@PathVariable("id") Long id,
            HttpServletRequest request,
            HttpServletResponse response) {
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

    @Deprecated
    @GetMapping("/getMetricTags")
    public Set<String> getMetricTags() {
        return metricService.getMetricTags();
    }

    @GetMapping("/getMetricClassifications")
    public Set<String> getMetricClassifications() {
        return metricService.getMetricTags();
    }

    @GetMapping("/getDrillDownDimension")
    public List<DrillDownDimension> getDrillDownDimension(Long metricId) {
        return metricService.getDrillDownDimension(metricId);
    }

    @PostMapping("/saveMetricQueryDefaultConfig")
    public boolean saveMetricQueryDefaultConfig(@RequestBody MetricQueryDefaultConfig queryDefaultConfig,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        metricService.saveMetricQueryDefaultConfig(queryDefaultConfig, user);
        return true;
    }

    @RequestMapping("getMetricQueryDefaultConfig/{metricId}")
    public MetricQueryDefaultConfig getMetricQueryDefaultConfig(@PathVariable("metricId") Long metricId,
            HttpServletRequest request,
            HttpServletResponse response) {
        User user = UserHolder.findUser(request, response);
        return metricService.getMetricQueryDefaultConfig(metricId, user);
    }

}
