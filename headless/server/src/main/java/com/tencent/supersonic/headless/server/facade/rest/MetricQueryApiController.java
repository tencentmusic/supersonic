package com.tencent.supersonic.headless.server.facade.rest;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.BatchDownloadReq;
import com.tencent.supersonic.headless.api.pojo.request.DownloadMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.server.facade.service.SemanticLayerService;
import com.tencent.supersonic.headless.server.service.DownloadService;
import com.tencent.supersonic.headless.server.service.MetricService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/semantic/query")
@Slf4j
public class MetricQueryApiController {

    @Autowired
    private SemanticLayerService semanticLayerService;

    @Autowired
    private MetricService metricService;

    @Autowired
    private DownloadService downloadService;

    @PostMapping("/metric")
    public Object queryByMetric(@RequestBody QueryMetricReq queryMetricReq,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        QueryStructReq queryStructReq = metricService.convert(queryMetricReq);
        return semanticLayerService.queryByReq(queryStructReq.convert(true), user);
    }

    @PostMapping("/download/metric")
    public void downloadMetric(@RequestBody DownloadMetricReq downloadMetricReq,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        downloadService.downloadByStruct(downloadMetricReq, user, response);
    }

    @PostMapping("/downloadBatch/metric")
    public void downloadBatch(@RequestBody BatchDownloadReq batchDownloadReq,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        downloadService.batchDownload(batchDownloadReq, user, response);
    }
}
