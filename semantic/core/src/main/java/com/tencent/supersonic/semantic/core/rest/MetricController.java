package com.tencent.supersonic.semantic.core.rest;


import com.github.pagehelper.PageInfo;
import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.semantic.api.core.request.MetricReq;
import com.tencent.supersonic.semantic.api.core.request.PageMetricReq;
import com.tencent.supersonic.semantic.api.core.response.MetricResp;
import com.tencent.supersonic.semantic.core.domain.MetricService;
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


    @GetMapping("/getMetricList/{domainId}")
    public List<MetricResp> getMetricList(@PathVariable("domainId") Long domainId) {
        return metricService.getMetrics(domainId);
    }


    @PostMapping("/queryMetric")
    public PageInfo<MetricResp> queryMetric(@RequestBody PageMetricReq pageMetrricReq) {
        return metricService.queryMetric(pageMetrricReq);
    }

    @GetMapping("getMetric/{domainId}/{bizName}")
    public MetricResp getMetric(@PathVariable("domainId") Long domainId, @PathVariable("bizName") String bizName) {
        return metricService.getMetric(domainId, bizName);
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


}
