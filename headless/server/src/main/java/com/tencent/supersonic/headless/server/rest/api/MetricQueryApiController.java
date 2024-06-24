package com.tencent.supersonic.headless.server.rest.api;

import com.tencent.supersonic.auth.api.authentication.pojo.User;
import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.headless.api.pojo.request.BatchDownloadReq;
import com.tencent.supersonic.headless.api.pojo.request.DownloadMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryMetricReq;
import com.tencent.supersonic.headless.api.pojo.request.QueryStructReq;
import com.tencent.supersonic.headless.server.service.DownloadService;
import com.tencent.supersonic.headless.server.service.MetricService;
import com.tencent.supersonic.headless.server.service.SemanticLayerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/semantic/query")
@Slf4j
public class MetricQueryApiController {

    @Autowired
    private SemanticLayerService queryService;

    @Autowired
    private MetricService metricService;

    @Autowired
    private DownloadService downloadService;

    @PostMapping("/metric")
    public Object queryByMetric(@RequestBody QueryMetricReq queryMetricReq,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        QueryStructReq queryStructReq = metricService.convert(queryMetricReq);
        return queryService.queryByReq(queryStructReq.convert(true), user);
    }

    @PostMapping("/download/metric")
    public void downloadMetric(@RequestBody DownloadMetricReq downloadMetricReq,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        downloadService.downloadByStruct(downloadMetricReq, user, response);
    }

    @PostMapping("/downloadBatch/metric")
    public void downloadBatch(@RequestBody BatchDownloadReq batchDownloadReq,
                              HttpServletRequest request,
                              HttpServletResponse response) throws Exception {
        User user = UserHolder.findUser(request, response);
        downloadService.batchDownload(batchDownloadReq, user, response);
    }

}
