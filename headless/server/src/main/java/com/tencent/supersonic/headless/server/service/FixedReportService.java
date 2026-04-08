package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.pojo.FixedReportVO;

import java.util.List;

public interface FixedReportService {

    List<FixedReportVO> listFixedReports(User user, String keyword, String domainName,
            String statusFilter, String viewFilter);

    void subscribe(Long datasetId, User user);

    void unsubscribe(Long datasetId, User user);

    Page<ReportExecutionDO> getExecutionsByDataset(Page<ReportExecutionDO> page, Long datasetId);
}
