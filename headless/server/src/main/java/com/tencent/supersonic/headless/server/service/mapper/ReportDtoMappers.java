package com.tencent.supersonic.headless.server.service.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.headless.api.pojo.request.ReportDeliveryConfigReq;
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleConfirmationReq;
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryConfigResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportDeliveryRecordResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportExecutionResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleConfirmationResp;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleResp;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryRecordDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleConfirmationDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DO <-> DTO mapping helpers for Report* service boundaries. One-way field copies via
 * {@link BeanUtils#copyProperties}; property names must match between the DO and DTO. Do not add
 * transformation logic here - if a field needs computed/derived values, the impl should enrich the
 * DTO after the mapping call.
 */
public final class ReportDtoMappers {

    private ReportDtoMappers() {}

    // ========== ReportSchedule ==========

    public static ReportScheduleResp toResp(ReportScheduleDO src) {
        if (src == null)
            return null;
        ReportScheduleResp dst = new ReportScheduleResp();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static ReportScheduleDO toDO(ReportScheduleReq src) {
        if (src == null)
            return null;
        ReportScheduleDO dst = new ReportScheduleDO();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static Page<ReportScheduleResp> toRespPage(Page<ReportScheduleDO> src) {
        Page<ReportScheduleResp> dst = new Page<>(src.getCurrent(), src.getSize(), src.getTotal());
        dst.setRecords(src.getRecords().stream().map(ReportDtoMappers::toResp)
                .collect(Collectors.toList()));
        return dst;
    }

    // ========== ReportDeliveryConfig ==========

    public static ReportDeliveryConfigResp toResp(ReportDeliveryConfigDO src) {
        if (src == null)
            return null;
        ReportDeliveryConfigResp dst = new ReportDeliveryConfigResp();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static ReportDeliveryConfigDO toDO(ReportDeliveryConfigReq src) {
        if (src == null)
            return null;
        ReportDeliveryConfigDO dst = new ReportDeliveryConfigDO();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static List<ReportDeliveryConfigResp> toConfigResps(List<ReportDeliveryConfigDO> src) {
        return src.stream().map(ReportDtoMappers::toResp).collect(Collectors.toList());
    }

    public static Page<ReportDeliveryConfigResp> toConfigRespPage(
            Page<ReportDeliveryConfigDO> src) {
        Page<ReportDeliveryConfigResp> dst =
                new Page<>(src.getCurrent(), src.getSize(), src.getTotal());
        dst.setRecords(toConfigResps(src.getRecords()));
        return dst;
    }

    // ========== ReportDeliveryRecord ==========

    public static ReportDeliveryRecordResp toResp(ReportDeliveryRecordDO src) {
        if (src == null)
            return null;
        ReportDeliveryRecordResp dst = new ReportDeliveryRecordResp();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static List<ReportDeliveryRecordResp> toRecordResps(List<ReportDeliveryRecordDO> src) {
        return src.stream().map(ReportDtoMappers::toResp).collect(Collectors.toList());
    }

    public static Page<ReportDeliveryRecordResp> toRecordRespPage(
            Page<ReportDeliveryRecordDO> src) {
        Page<ReportDeliveryRecordResp> dst =
                new Page<>(src.getCurrent(), src.getSize(), src.getTotal());
        dst.setRecords(toRecordResps(src.getRecords()));
        return dst;
    }

    // ========== ReportExecution ==========

    public static ReportExecutionResp toResp(ReportExecutionDO src) {
        if (src == null)
            return null;
        ReportExecutionResp dst = new ReportExecutionResp();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static Page<ReportExecutionResp> toExecutionRespPage(Page<ReportExecutionDO> src) {
        Page<ReportExecutionResp> dst = new Page<>(src.getCurrent(), src.getSize(), src.getTotal());
        dst.setRecords(src.getRecords().stream().map(ReportDtoMappers::toResp)
                .collect(Collectors.toList()));
        return dst;
    }

    // ========== ReportScheduleConfirmation ==========

    public static ReportScheduleConfirmationResp toResp(ReportScheduleConfirmationDO src) {
        if (src == null)
            return null;
        ReportScheduleConfirmationResp dst = new ReportScheduleConfirmationResp();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }

    public static ReportScheduleConfirmationDO toDO(ReportScheduleConfirmationReq src) {
        if (src == null)
            return null;
        ReportScheduleConfirmationDO dst = new ReportScheduleConfirmationDO();
        BeanUtils.copyProperties(src, dst);
        return dst;
    }
}
