package com.tencent.supersonic.headless.server.service;

import javax.servlet.http.HttpServletResponse;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.api.pojo.request.BatchDownloadReq;
import com.tencent.supersonic.headless.api.pojo.request.DownloadMetricReq;

public interface DownloadService {

    void downloadByStruct(DownloadMetricReq downloadStructReq, User user,
            HttpServletResponse response) throws Exception;

    void batchDownload(BatchDownloadReq batchDownloadReq, User user, HttpServletResponse response)
            throws Exception;
}
