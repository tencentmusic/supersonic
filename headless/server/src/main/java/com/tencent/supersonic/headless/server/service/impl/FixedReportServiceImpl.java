package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportSubscriptionDO;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportDeliveryConfigMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportExecutionMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportSubscriptionMapper;
import com.tencent.supersonic.headless.server.pojo.FixedReportVO;
import com.tencent.supersonic.headless.server.pojo.SemanticDeployment;
import com.tencent.supersonic.headless.server.service.FixedReportService;
import com.tencent.supersonic.headless.server.service.SemanticTemplateService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FixedReportServiceImpl implements FixedReportService {

    private final SemanticTemplateService semanticTemplateService;
    private final ReportScheduleMapper scheduleMapper;
    private final ReportExecutionMapper executionMapper;
    private final ReportDeliveryConfigMapper deliveryConfigMapper;
    private final ReportSubscriptionMapper subscriptionMapper;

    @Override
    public List<FixedReportVO> listFixedReports(User user, String keyword, String domainName,
            String statusFilter, String viewFilter) {

        // 1. Get all successful deployments (each = one fixed report)
        List<SemanticDeployment> deployments =
                semanticTemplateService.getDeploymentHistory(user).stream()
                        .filter(d -> d.getStatus() == SemanticDeployment.DeploymentStatus.SUCCESS)
                        .filter(d -> d.getResultDetail() != null
                                && d.getResultDetail().getDataSetId() != null)
                        .collect(Collectors.toList());

        if (deployments.isEmpty()) {
            return Collections.emptyList();
        }

        // Deduplicate by datasetId - keep the latest deployment per dataset
        Map<Long, SemanticDeployment> latestByDataset = deployments.stream()
                .collect(Collectors.toMap(d -> d.getResultDetail().getDataSetId(), d -> d,
                        (a, b) -> (a.getId() > b.getId()) ? a : b));

        Set<Long> datasetIds = latestByDataset.keySet();

        // 2. Load all schedules for these datasets
        List<ReportScheduleDO> allSchedules =
                scheduleMapper.selectList(new LambdaQueryWrapper<ReportScheduleDO>()
                        .in(ReportScheduleDO::getDatasetId, datasetIds));
        Map<Long, List<ReportScheduleDO>> schedulesByDataset = allSchedules.stream()
                .collect(Collectors.groupingBy(ReportScheduleDO::getDatasetId));

        // 3. Load latest execution per schedule
        Set<Long> scheduleIds =
                allSchedules.stream().map(ReportScheduleDO::getId).collect(Collectors.toSet());
        Map<Long, ReportExecutionDO> latestExecBySchedule = loadLatestExecutions(scheduleIds);

        // 3b. Batch-load previous SUCCESS time for schedules whose latest exec is FAILED
        Set<Long> failedScheduleIds = latestExecBySchedule.entrySet().stream()
                .filter(e -> "FAILED".equals(e.getValue().getStatus())).map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Map<Long, Date> latestSuccessBySchedule = loadLatestSuccessTime(failedScheduleIds);

        // 4. Load delivery configs for only the referenced configIds
        Set<Long> allConfigIds = allSchedules.stream()
                .filter(s -> StringUtils.isNotBlank(s.getDeliveryConfigIds()))
                .flatMap(s -> Arrays.stream(s.getDeliveryConfigIds().split(","))).map(String::trim)
                .filter(StringUtils::isNotBlank).map(Long::valueOf).collect(Collectors.toSet());
        Map<Long, ReportDeliveryConfigDO> deliveryConfigMap =
                allConfigIds.isEmpty() ? Collections.emptyMap()
                        : deliveryConfigMapper
                                .selectList(new LambdaQueryWrapper<ReportDeliveryConfigDO>()
                                        .in(ReportDeliveryConfigDO::getId, allConfigIds))
                                .stream().collect(Collectors.toMap(ReportDeliveryConfigDO::getId,
                                        c -> c, (a, b) -> a));

        // 5. Load subscriptions for current user
        Set<Long> subscribedDatasets = subscriptionMapper
                .selectList(new LambdaQueryWrapper<ReportSubscriptionDO>()
                        .eq(ReportSubscriptionDO::getUserId, user.getId())
                        .in(ReportSubscriptionDO::getDatasetId, datasetIds))
                .stream().map(ReportSubscriptionDO::getDatasetId).collect(Collectors.toSet());

        // 6. Assemble VO list
        List<FixedReportVO> results = new ArrayList<>();
        for (Map.Entry<Long, SemanticDeployment> entry : latestByDataset.entrySet()) {
            Long dsId = entry.getKey();
            SemanticDeployment dep = entry.getValue();
            FixedReportVO vo = buildVO(dep, dsId, schedulesByDataset.getOrDefault(dsId, List.of()),
                    latestExecBySchedule, latestSuccessBySchedule, deliveryConfigMap,
                    subscribedDatasets.contains(dsId));
            results.add(vo);
        }

        // 7. Apply filters
        results = applyFilters(results, keyword, domainName, statusFilter, viewFilter);

        return results;
    }

    @Override
    public void subscribe(Long datasetId, User user) {
        LambdaQueryWrapper<ReportSubscriptionDO> wrapper =
                new LambdaQueryWrapper<ReportSubscriptionDO>()
                        .eq(ReportSubscriptionDO::getUserId, user.getId())
                        .eq(ReportSubscriptionDO::getDatasetId, datasetId);
        if (subscriptionMapper.selectCount(wrapper) > 0) {
            return; // already subscribed
        }
        ReportSubscriptionDO sub = new ReportSubscriptionDO();
        sub.setUserId(user.getId());
        sub.setDatasetId(datasetId);
        sub.setTenantId(user.getTenantId());
        subscriptionMapper.insert(sub);
    }

    @Override
    public void unsubscribe(Long datasetId, User user) {
        subscriptionMapper.delete(new LambdaQueryWrapper<ReportSubscriptionDO>()
                .eq(ReportSubscriptionDO::getUserId, user.getId())
                .eq(ReportSubscriptionDO::getDatasetId, datasetId));
    }

    @Override
    public Page<ReportExecutionDO> getExecutionsByDataset(Page<ReportExecutionDO> page,
            Long datasetId) {
        List<Long> scheduleIds = scheduleMapper
                .selectList(new LambdaQueryWrapper<ReportScheduleDO>()
                        .eq(ReportScheduleDO::getDatasetId, datasetId)
                        .select(ReportScheduleDO::getId))
                .stream().map(ReportScheduleDO::getId).collect(Collectors.toList());
        if (scheduleIds.isEmpty()) {
            page.setRecords(List.of());
            page.setTotal(0L);
            return page;
        }
        LambdaQueryWrapper<ReportExecutionDO> wrapper = new LambdaQueryWrapper<ReportExecutionDO>()
                .in(ReportExecutionDO::getScheduleId, scheduleIds)
                .orderByDesc(ReportExecutionDO::getStartTime);
        return executionMapper.selectPage(page, wrapper);
    }

    // --- Internal helpers ---

    private Map<Long, ReportExecutionDO> loadLatestExecutions(Set<Long> scheduleIds) {
        if (scheduleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // Get the latest execution per schedule: fetch ordered by startTime desc, then deduplicate
        List<ReportExecutionDO> execs =
                executionMapper.selectList(new LambdaQueryWrapper<ReportExecutionDO>()
                        .in(ReportExecutionDO::getScheduleId, scheduleIds)
                        .orderByDesc(ReportExecutionDO::getStartTime));
        return execs.stream().collect(Collectors.toMap(ReportExecutionDO::getScheduleId, e -> e,
                (first, second) -> first)); // keep first = latest
    }

    private Map<Long, Date> loadLatestSuccessTime(Set<Long> scheduleIds) {
        if (scheduleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ReportExecutionDO> execs =
                executionMapper.selectList(new LambdaQueryWrapper<ReportExecutionDO>()
                        .in(ReportExecutionDO::getScheduleId, scheduleIds)
                        .eq(ReportExecutionDO::getStatus, "SUCCESS")
                        .orderByDesc(ReportExecutionDO::getStartTime));
        // keep only the latest SUCCESS per schedule
        return execs.stream().collect(Collectors.toMap(ReportExecutionDO::getScheduleId,
                ReportExecutionDO::getStartTime, (first, second) -> first));
    }

    private FixedReportVO buildVO(SemanticDeployment dep, Long datasetId,
            List<ReportScheduleDO> schedules, Map<Long, ReportExecutionDO> latestExecBySchedule,
            Map<Long, Date> latestSuccessBySchedule,
            Map<Long, ReportDeliveryConfigDO> deliveryConfigMap, boolean subscribed) {

        FixedReportVO vo = new FixedReportVO();
        vo.setDeploymentId(dep.getId());
        vo.setDatasetId(datasetId);
        vo.setReportName(dep.getTemplateName() != null ? dep.getTemplateName()
                : (dep.getResultDetail().getDataSetName() != null
                        ? dep.getResultDetail().getDataSetName()
                        : "Report #" + dep.getId()));
        vo.setDomainName(dep.getResultDetail().getDomainName());
        vo.setDescription(dep.getTemplateConfigSnapshot() != null
                && dep.getTemplateConfigSnapshot().getDataSet() != null
                        ? dep.getTemplateConfigSnapshot().getDataSet().getDescription()
                        : null);
        vo.setSubscribed(subscribed);
        vo.setScheduleCount(schedules.size());
        vo.setEnabledScheduleCount(
                (int) schedules.stream().filter(s -> Boolean.TRUE.equals(s.getEnabled())).count());

        // Find latest execution across all schedules for this dataset
        ReportExecutionDO latestExec =
                schedules.stream().map(s -> latestExecBySchedule.get(s.getId()))
                        .filter(Objects::nonNull).max((a, b) -> {
                            Date ta = a.getStartTime();
                            Date tb = b.getStartTime();
                            if (ta == null && tb == null)
                                return 0;
                            if (ta == null)
                                return -1;
                            if (tb == null)
                                return 1;
                            return ta.compareTo(tb);
                        }).orElse(null);

        if (latestExec != null) {
            vo.setLatestResultTime(latestExec.getStartTime());
            vo.setLatestResultStatus(latestExec.getStatus());
            vo.setLatestErrorMessage(latestExec.getErrorMessage());
            vo.setLatestRowCount(latestExec.getRowCount());
        }

        // Check result expiry: expired if latest result > 48h old
        if (vo.getLatestResultTime() != null) {
            long ageMs = System.currentTimeMillis() - vo.getLatestResultTime().getTime();
            vo.setResultExpired(ageMs > 48 * 3600 * 1000L);
        }

        // If latest is FAILED, look up previous SUCCESS time from the pre-fetched batch map
        if ("FAILED".equals(vo.getLatestResultStatus())) {
            schedules.stream().map(ReportScheduleDO::getId).map(latestSuccessBySchedule::get)
                    .filter(Objects::nonNull).max(Date::compareTo)
                    .ifPresent(vo::setPreviousSuccessTime);
        }

        // Collect delivery channels from all schedules
        Set<Long> configIds = schedules.stream()
                .filter(s -> StringUtils.isNotBlank(s.getDeliveryConfigIds()))
                .flatMap(s -> Arrays.stream(s.getDeliveryConfigIds().split(","))).map(String::trim)
                .filter(StringUtils::isNotBlank).map(Long::valueOf).collect(Collectors.toSet());

        List<FixedReportVO.DeliverySummaryItem> channels = configIds.stream().map(id -> {
            ReportDeliveryConfigDO config = deliveryConfigMap.get(id);
            if (config == null)
                return null;
            FixedReportVO.DeliverySummaryItem item = new FixedReportVO.DeliverySummaryItem();
            item.setConfigId(config.getId());
            item.setConfigName(config.getName());
            item.setDeliveryType(config.getDeliveryType());
            item.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
            return item;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        vo.setDeliveryChannels(channels);

        // Derive consumption status
        vo.setConsumptionStatus(deriveConsumptionStatus(vo));

        return vo;
    }

    /**
     * Derive the consumption status for a fixed report VO. Public static for unit testability
     * without mocking.
     *
     * Priority order: NO_RESULT > RECENTLY_FAILED > EXPIRED > NO_DELIVERY > PARTIAL_CHANNEL_ERROR >
     * AVAILABLE
     */
    public static String deriveConsumptionStatus(FixedReportVO vo) {
        if (vo.getLatestResultStatus() == null && vo.getLatestResultTime() == null) {
            return "NO_RESULT";
        }
        if ("FAILED".equals(vo.getLatestResultStatus())) {
            return "RECENTLY_FAILED";
        }
        if (vo.isResultExpired()) {
            return "EXPIRED";
        }
        if (vo.getDeliveryChannels() != null && vo.getDeliveryChannels().isEmpty()
                && vo.getScheduleCount() > 0) {
            return "NO_DELIVERY";
        }
        if (vo.getDeliveryChannels() != null
                && vo.getDeliveryChannels().stream().anyMatch(c -> !c.isEnabled())) {
            return "PARTIAL_CHANNEL_ERROR";
        }
        return "AVAILABLE";
    }

    private List<FixedReportVO> applyFilters(List<FixedReportVO> list, String keyword,
            String domainName, String statusFilter, String viewFilter) {
        return list.stream()
                .filter(vo -> StringUtils.isBlank(keyword)
                        || StringUtils.containsIgnoreCase(vo.getReportName(), keyword)
                        || StringUtils.containsIgnoreCase(vo.getDescription(), keyword))
                .filter(vo -> StringUtils.isBlank(domainName)
                        || domainName.equals(vo.getDomainName()))
                .filter(vo -> StringUtils.isBlank(statusFilter)
                        || statusFilter.equals(vo.getConsumptionStatus()))
                .filter(vo -> {
                    if ("subscribed".equals(viewFilter))
                        return vo.isSubscribed();
                    return true; // "all" or blank
                }).collect(Collectors.toList());
    }
}
