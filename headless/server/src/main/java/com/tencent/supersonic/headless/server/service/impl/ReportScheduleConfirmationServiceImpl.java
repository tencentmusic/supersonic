package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.supersonic.headless.api.pojo.request.ReportScheduleConfirmationReq;
import com.tencent.supersonic.headless.api.pojo.response.ReportScheduleConfirmationResp;
import com.tencent.supersonic.headless.api.service.ReportScheduleConfirmationService;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleConfirmationDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleConfirmationMapper;
import com.tencent.supersonic.headless.server.service.mapper.ReportDtoMappers;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class ReportScheduleConfirmationServiceImpl
        extends ServiceImpl<ReportScheduleConfirmationMapper, ReportScheduleConfirmationDO>
        implements ReportScheduleConfirmationService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Override
    public ReportScheduleConfirmationResp createPending(ReportScheduleConfirmationReq req) {
        ReportScheduleConfirmationDO confirmation = ReportDtoMappers.toDO(req);
        Date now = new Date();
        if (confirmation.getConfirmToken() == null || confirmation.getConfirmToken().isBlank()) {
            confirmation.setConfirmToken(UUID.randomUUID().toString().replace("-", ""));
        }
        if (confirmation.getCreatedAt() == null) {
            confirmation.setCreatedAt(now);
        }
        if (confirmation.getStatus() == null || confirmation.getStatus().isBlank()) {
            confirmation.setStatus(STATUS_PENDING);
        }
        UpdateWrapper<ReportScheduleConfirmationDO> cancelWrapper = new UpdateWrapper<>();
        cancelWrapper.lambda().eq(ReportScheduleConfirmationDO::getUserId, confirmation.getUserId())
                .eq(ReportScheduleConfirmationDO::getChatId, confirmation.getChatId())
                .eq(ReportScheduleConfirmationDO::getStatus, STATUS_PENDING)
                .set(ReportScheduleConfirmationDO::getStatus, STATUS_CANCELLED);
        baseMapper.update(null, cancelWrapper);
        baseMapper.insert(confirmation);
        return ReportDtoMappers.toResp(confirmation);
    }

    @Override
    public ReportScheduleConfirmationResp getLatestPending(Long userId, Integer chatId) {
        return ReportDtoMappers.toResp(getLatestPendingInternal(userId, chatId));
    }

    private ReportScheduleConfirmationDO getLatestPendingInternal(Long userId, Integer chatId) {
        if (userId == null || chatId == null) {
            return null;
        }
        QueryWrapper<ReportScheduleConfirmationDO> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(ReportScheduleConfirmationDO::getUserId, userId)
                .eq(ReportScheduleConfirmationDO::getChatId, chatId)
                .eq(ReportScheduleConfirmationDO::getStatus, STATUS_PENDING)
                .orderByDesc(ReportScheduleConfirmationDO::getCreatedAt);
        wrapper.last("limit 1");
        ReportScheduleConfirmationDO confirmation = baseMapper.selectOne(wrapper);
        if (confirmation == null) {
            return null;
        }
        if (confirmation.getExpireAt() != null && confirmation.getExpireAt().before(new Date())) {
            updateStatus(confirmation.getId(), STATUS_EXPIRED);
            return null;
        }
        return confirmation;
    }

    @Override
    public boolean hasPending(Long userId, Integer chatId) {
        return getLatestPendingInternal(userId, chatId) != null;
    }

    @Override
    public void updateStatus(Long id, String status) {
        if (id == null || status == null) {
            return;
        }
        UpdateWrapper<ReportScheduleConfirmationDO> wrapper = new UpdateWrapper<>();
        wrapper.lambda().eq(ReportScheduleConfirmationDO::getId, id)
                .set(ReportScheduleConfirmationDO::getStatus, status);
        baseMapper.update(null, wrapper);
    }
}
