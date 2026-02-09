package com.tencent.supersonic.headless.server.service.impl;

import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.pojo.ExecutionSource;
import com.tencent.supersonic.headless.server.pojo.OutputConfig;
import com.tencent.supersonic.headless.server.pojo.OutputFormat;
import com.tencent.supersonic.headless.server.pojo.ReportExecuteReq;
import com.tencent.supersonic.headless.server.pojo.ReportExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ReportExecutionContextBuilder {

    public ReportExecutionContext build(ReportExecuteReq req, User user) {
        OutputFormat format =
                req.getOutputFormat() != null ? req.getOutputFormat() : OutputFormat.JSON;
        return ReportExecutionContext.builder().tenantId(user.getTenantId())
                .datasetId(req.getDatasetId()).operatorUserId(user.getId())
                .source(ExecutionSource.WEB).queryConfig(req.getQueryConfig())
                .resolvedParams(req.getParams())
                .outputConfig(OutputConfig.builder().format(format).async(false).build()).build();
    }

    public ReportExecutionContext buildManualFromSchedule(ReportScheduleDO schedule, User user) {
        OutputFormat format;
        try {
            format = OutputFormat.valueOf(schedule.getOutputFormat());
        } catch (Exception e) {
            format = OutputFormat.EXCEL;
        }
        return ReportExecutionContext.builder().tenantId(user.getTenantId())
                .datasetId(schedule.getDatasetId()).scheduleId(schedule.getId())
                .scheduleName(schedule.getName()).operatorUserId(user.getId())
                .source(ExecutionSource.MANUAL).queryConfig(schedule.getQueryConfig())
                .templateVersion(schedule.getTemplateVersion())
                .deliveryConfigIds(parseDeliveryConfigIds(schedule.getDeliveryConfigIds()))
                .outputConfig(OutputConfig.builder().format(format).async(false).build()).build();
    }

    public ReportExecutionContext buildFromSchedule(ReportScheduleDO schedule, int attempt) {
        OutputFormat format;
        try {
            format = OutputFormat.valueOf(schedule.getOutputFormat());
        } catch (Exception e) {
            format = OutputFormat.EXCEL;
        }
        return ReportExecutionContext.builder().tenantId(schedule.getTenantId())
                .datasetId(schedule.getDatasetId()).scheduleId(schedule.getId())
                .scheduleName(schedule.getName()).operatorUserId(schedule.getOwnerId())
                .source(ExecutionSource.SCHEDULE).queryConfig(schedule.getQueryConfig())
                .templateVersion(schedule.getTemplateVersion())
                .deliveryConfigIds(parseDeliveryConfigIds(schedule.getDeliveryConfigIds()))
                .outputConfig(OutputConfig.builder().format(format).async(false).build()).build();
    }

    /**
     * Parse comma-separated delivery config IDs string into a list.
     */
    private List<Long> parseDeliveryConfigIds(String deliveryConfigIds) {
        if (StringUtils.isBlank(deliveryConfigIds)) {
            return new ArrayList<>();
        }
        try {
            return Arrays.stream(deliveryConfigIds.split(",")).map(String::trim)
                    .filter(StringUtils::isNotBlank).map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse delivery config IDs: {}", deliveryConfigIds);
            return new ArrayList<>();
        }
    }
}
