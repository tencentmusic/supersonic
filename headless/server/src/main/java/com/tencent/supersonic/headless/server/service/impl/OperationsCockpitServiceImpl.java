package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertEventDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.BusinessTopicDO;
import com.tencent.supersonic.headless.server.persistence.mapper.AlertEventMapper;
import com.tencent.supersonic.headless.server.pojo.BusinessTopicVO;
import com.tencent.supersonic.headless.server.pojo.FixedReportVO;
import com.tencent.supersonic.headless.server.pojo.OperationsCockpitVO;
import com.tencent.supersonic.headless.server.service.BusinessTopicService;
import com.tencent.supersonic.headless.server.service.FixedReportService;
import com.tencent.supersonic.headless.server.service.OperationsCockpitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperationsCockpitServiceImpl implements OperationsCockpitService {

    private static final int MAX_TOPICS = 30;
    private static final int MAX_REPORTS = 12;
    private static final int MAX_EVENTS = 10;
    private static final int MAX_RISKS = 10;

    private static final Set<String> PENDING_RESOLUTION = Set.of("OPEN", "CONFIRMED", "ASSIGNED");
    private static final Set<String> RISK_STATUSES = Set.of("RECENTLY_FAILED", "EXPIRED",
            "PARTIAL_CHANNEL_ERROR", "NO_RESULT", "NO_DELIVERY");

    private final BusinessTopicService businessTopicService;
    private final FixedReportService fixedReportService;
    private final AlertEventMapper alertEventMapper;

    @Override
    public OperationsCockpitVO getCockpit(User user) {
        OperationsCockpitVO vo = new OperationsCockpitVO();

        Page<BusinessTopicDO> topicPage = new Page<>(1, MAX_TOPICS);
        Page<BusinessTopicVO> topicVoPage = businessTopicService.listTopics(topicPage, true, user);
        for (BusinessTopicVO full : topicVoPage.getRecords()) {
            OperationsCockpitVO.TopicSummary ts = new OperationsCockpitVO.TopicSummary();
            ts.setId(full.getId());
            ts.setName(full.getName());
            ts.setDescription(full.getDescription());
            ts.setPriority(full.getPriority());
            ts.setFixedReportCount(full.getFixedReportCount());
            ts.setAlertRuleCount(full.getAlertRuleCount());
            ts.setScheduleCount(full.getScheduleCount());
            vo.getTopics().add(ts);
        }

        List<FixedReportVO> allReports =
                fixedReportService.listFixedReports(user, null, null, null, null);
        List<FixedReportVO> subscribed = allReports.stream().filter(FixedReportVO::isSubscribed)
                .collect(Collectors.toList());
        List<FixedReportVO> keyPool = !subscribed.isEmpty() ? subscribed : allReports;
        keyPool = keyPool.stream()
                .sorted(Comparator.comparing(FixedReportVO::getLatestResultTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_REPORTS).collect(Collectors.toList());
        for (FixedReportVO r : keyPool) {
            vo.getKeyReports().add(toReportSummary(r));
        }

        vo.setPendingAlertEventCount(
                alertEventMapper.selectCount(pendingEventsWrapper(user, false)));
        LambdaQueryWrapper<AlertEventDO> eventWrapper = pendingEventsWrapper(user, true);
        Page<AlertEventDO> eventPage = new Page<>(1, MAX_EVENTS);
        Page<AlertEventDO> events = alertEventMapper.selectPage(eventPage, eventWrapper);

        for (AlertEventDO e : events.getRecords()) {
            OperationsCockpitVO.AlertEventSummary es = new OperationsCockpitVO.AlertEventSummary();
            es.setId(e.getId());
            es.setRuleId(e.getRuleId());
            es.setSeverity(e.getSeverity());
            es.setResolutionStatus(
                    e.getResolutionStatus() != null ? e.getResolutionStatus() : "OPEN");
            es.setMessage(e.getMessage());
            es.setCreatedAt(e.getCreatedAt());
            vo.getPendingAlertEvents().add(es);
        }

        List<FixedReportVO> risks = allReports.stream()
                .filter(r -> r.getConsumptionStatus() != null && RISK_STATUSES
                        .contains(r.getConsumptionStatus().toUpperCase(Locale.ROOT)))
                .sorted(Comparator.comparing(FixedReportVO::getLatestResultTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_RISKS).collect(Collectors.toList());
        for (FixedReportVO r : risks) {
            vo.getReliabilityRisks().add(toReportSummary(r));
        }

        return vo;
    }

    private static LambdaQueryWrapper<AlertEventDO> pendingEventsWrapper(User user,
            boolean orderByCreatedDesc) {
        LambdaQueryWrapper<AlertEventDO> w = new LambdaQueryWrapper<>();
        if (user.getTenantId() != null) {
            w.eq(AlertEventDO::getTenantId, user.getTenantId());
        }
        w.and(q -> q.in(AlertEventDO::getResolutionStatus, PENDING_RESOLUTION).or()
                .isNull(AlertEventDO::getResolutionStatus));
        if (orderByCreatedDesc) {
            w.orderByDesc(AlertEventDO::getCreatedAt);
        }
        return w;
    }

    private static OperationsCockpitVO.FixedReportSummary toReportSummary(FixedReportVO r) {
        OperationsCockpitVO.FixedReportSummary s = new OperationsCockpitVO.FixedReportSummary();
        s.setDatasetId(r.getDatasetId());
        s.setReportName(r.getReportName());
        s.setDomainName(r.getDomainName());
        s.setConsumptionStatus(r.getConsumptionStatus());
        s.setLatestResultTime(r.getLatestResultTime());
        s.setSubscribed(r.isSubscribed());
        return s;
    }
}
