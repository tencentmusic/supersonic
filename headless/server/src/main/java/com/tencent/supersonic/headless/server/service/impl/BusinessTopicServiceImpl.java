package com.tencent.supersonic.headless.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertRuleDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.BusinessTopicDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.BusinessTopicItemDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.persistence.mapper.AlertRuleMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.BusinessTopicItemMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.BusinessTopicMapper;
import com.tencent.supersonic.headless.server.persistence.mapper.ReportScheduleMapper;
import com.tencent.supersonic.headless.server.pojo.BusinessTopicVO;
import com.tencent.supersonic.headless.server.pojo.FixedReportVO;
import com.tencent.supersonic.headless.server.service.BusinessTopicService;
import com.tencent.supersonic.headless.server.service.FixedReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessTopicServiceImpl implements BusinessTopicService {

    private final BusinessTopicMapper topicMapper;
    private final BusinessTopicItemMapper itemMapper;
    private final FixedReportService fixedReportService;
    private final ReportScheduleMapper scheduleMapper;
    private final AlertRuleMapper alertRuleMapper;

    @Override
    public Page<BusinessTopicVO> listTopics(Page<BusinessTopicDO> page, Boolean enabled,
            User user) {
        LambdaQueryWrapper<BusinessTopicDO> wrapper = new LambdaQueryWrapper<>();
        if (enabled != null) {
            wrapper.eq(BusinessTopicDO::getEnabled, enabled ? 1 : 0);
        }
        wrapper.orderByAsc(BusinessTopicDO::getPriority);
        Page<BusinessTopicDO> doPage = topicMapper.selectPage(page, wrapper);

        // Batch load item counts
        List<Long> topicIds = doPage.getRecords().stream().map(BusinessTopicDO::getId)
                .collect(Collectors.toList());
        Map<Long, List<BusinessTopicItemDO>> itemsByTopic =
                topicIds.isEmpty() ? Collections.emptyMap()
                        : itemMapper
                                .selectList(new LambdaQueryWrapper<BusinessTopicItemDO>()
                                        .in(BusinessTopicItemDO::getTopicId, topicIds))
                                .stream()
                                .collect(Collectors.groupingBy(BusinessTopicItemDO::getTopicId));

        Page<BusinessTopicVO> voPage =
                new Page<>(doPage.getCurrent(), doPage.getSize(), doPage.getTotal());
        voPage.setRecords(doPage.getRecords().stream().map(topic -> {
            BusinessTopicVO vo = toVO(topic);
            List<BusinessTopicItemDO> items = itemsByTopic.getOrDefault(topic.getId(), List.of());
            vo.setFixedReportCount((int) items.stream()
                    .filter(i -> "FIXED_REPORT".equals(i.getItemType())).count());
            vo.setAlertRuleCount(
                    (int) items.stream().filter(i -> "ALERT_RULE".equals(i.getItemType())).count());
            vo.setScheduleCount(
                    (int) items.stream().filter(i -> "SCHEDULE".equals(i.getItemType())).count());
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public BusinessTopicVO getTopicDetail(Long id, User user) {
        BusinessTopicDO topic = topicMapper.selectById(id);
        if (topic == null) {
            return null;
        }
        BusinessTopicVO vo = toVO(topic);
        List<BusinessTopicItemDO> itemDOs =
                itemMapper.selectList(new LambdaQueryWrapper<BusinessTopicItemDO>()
                        .eq(BusinessTopicItemDO::getTopicId, id));

        // Resolve item names
        List<BusinessTopicVO.TopicItem> items = new ArrayList<>();
        Map<String, Set<Long>> idsByType =
                itemDOs.stream().collect(Collectors.groupingBy(BusinessTopicItemDO::getItemType,
                        Collectors.mapping(BusinessTopicItemDO::getItemId, Collectors.toSet())));

        // Fixed reports
        Set<Long> reportDatasetIds = idsByType.getOrDefault("FIXED_REPORT", Set.of());
        if (!reportDatasetIds.isEmpty()) {
            List<FixedReportVO> reports =
                    fixedReportService.listFixedReports(user, null, null, null, null);
            Map<Long, String> reportNames = reports.stream().collect(Collectors
                    .toMap(FixedReportVO::getDatasetId, FixedReportVO::getReportName, (a, b) -> a));
            reportDatasetIds.forEach(dsId -> {
                BusinessTopicVO.TopicItem item = new BusinessTopicVO.TopicItem();
                item.setItemType("FIXED_REPORT");
                item.setItemId(dsId);
                item.setItemName(reportNames.getOrDefault(dsId, "Report #" + dsId));
                items.add(item);
            });
        }

        // Alert rules
        Set<Long> ruleIds = idsByType.getOrDefault("ALERT_RULE", Set.of());
        if (!ruleIds.isEmpty()) {
            alertRuleMapper
                    .selectList(
                            new LambdaQueryWrapper<AlertRuleDO>().in(AlertRuleDO::getId, ruleIds))
                    .forEach(rule -> {
                        BusinessTopicVO.TopicItem item = new BusinessTopicVO.TopicItem();
                        item.setItemType("ALERT_RULE");
                        item.setItemId(rule.getId());
                        item.setItemName(rule.getName());
                        items.add(item);
                    });
        }

        // Schedules
        Set<Long> scheduleIds = idsByType.getOrDefault("SCHEDULE", Set.of());
        if (!scheduleIds.isEmpty()) {
            scheduleMapper.selectList(new LambdaQueryWrapper<ReportScheduleDO>()
                    .in(ReportScheduleDO::getId, scheduleIds)).forEach(sched -> {
                        BusinessTopicVO.TopicItem item = new BusinessTopicVO.TopicItem();
                        item.setItemType("SCHEDULE");
                        item.setItemId(sched.getId());
                        item.setItemName(sched.getName());
                        items.add(item);
                    });
        }

        vo.setItems(items);
        vo.setFixedReportCount(
                (int) items.stream().filter(i -> "FIXED_REPORT".equals(i.getItemType())).count());
        vo.setAlertRuleCount(
                (int) items.stream().filter(i -> "ALERT_RULE".equals(i.getItemType())).count());
        vo.setScheduleCount(
                (int) items.stream().filter(i -> "SCHEDULE".equals(i.getItemType())).count());
        return vo;
    }

    @Override
    public BusinessTopicDO createTopic(BusinessTopicDO topic, User user) {
        topic.setCreatedBy(user.getName());
        topic.setTenantId(user.getTenantId());
        if (topic.getEnabled() == null) {
            topic.setEnabled(1);
        }
        if (topic.getPriority() == null) {
            topic.setPriority(0);
        }
        topicMapper.insert(topic);
        return topic;
    }

    @Override
    public BusinessTopicDO updateTopic(BusinessTopicDO topic, User user) {
        topic.setUpdatedBy(user.getName());
        topicMapper.updateById(topic);
        return topicMapper.selectById(topic.getId());
    }

    @Override
    @Transactional
    public void deleteTopic(Long id, User user) {
        topicMapper.deleteById(id);
        itemMapper.delete(new LambdaQueryWrapper<BusinessTopicItemDO>()
                .eq(BusinessTopicItemDO::getTopicId, id));
    }

    @Override
    public void addItems(Long topicId, List<String> itemTypes, List<Long> itemIds, User user) {
        BusinessTopicDO topic = topicMapper.selectById(topicId);
        if (topic == null) {
            return;
        }
        for (int i = 0; i < itemTypes.size(); i++) {
            BusinessTopicItemDO item = new BusinessTopicItemDO();
            item.setTopicId(topicId);
            item.setItemType(itemTypes.get(i));
            item.setItemId(itemIds.get(i));
            item.setTenantId(topic.getTenantId());
            long existing = itemMapper.selectCount(new LambdaQueryWrapper<BusinessTopicItemDO>()
                    .eq(BusinessTopicItemDO::getTopicId, topicId)
                    .eq(BusinessTopicItemDO::getItemType, itemTypes.get(i))
                    .eq(BusinessTopicItemDO::getItemId, itemIds.get(i)));
            if (existing == 0) {
                itemMapper.insert(item);
            }
        }
    }

    @Override
    public void removeItem(Long topicId, String itemType, Long itemId, User user) {
        itemMapper.delete(new LambdaQueryWrapper<BusinessTopicItemDO>()
                .eq(BusinessTopicItemDO::getTopicId, topicId)
                .eq(BusinessTopicItemDO::getItemType, itemType)
                .eq(BusinessTopicItemDO::getItemId, itemId));
    }

    private BusinessTopicVO toVO(BusinessTopicDO topic) {
        BusinessTopicVO vo = new BusinessTopicVO();
        vo.setId(topic.getId());
        vo.setName(topic.getName());
        vo.setDescription(topic.getDescription());
        vo.setPriority(topic.getPriority());
        vo.setOwnerId(topic.getOwnerId());
        vo.setDefaultDeliveryConfigIds(topic.getDefaultDeliveryConfigIds());
        vo.setEnabled(topic.getEnabled());
        vo.setCreatedAt(topic.getCreatedAt());
        vo.setUpdatedAt(topic.getUpdatedAt());
        vo.setCreatedBy(topic.getCreatedBy());
        return vo;
    }
}
