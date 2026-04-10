package com.tencent.supersonic.headless.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertEventDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertExecutionDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.AlertRuleDO;
import com.tencent.supersonic.headless.server.pojo.AlertEventTransitionReq;
import com.tencent.supersonic.headless.server.service.impl.AlertEvaluator;

import java.util.List;
import java.util.Map;

public interface AlertRuleService {

    AlertRuleDO createRule(AlertRuleDO rule);

    AlertRuleDO updateRule(AlertRuleDO rule);

    void deleteRule(Long id);

    AlertRuleDO getRuleById(Long id);

    Page<AlertRuleDO> getRuleList(Page<AlertRuleDO> page, Long datasetId, Boolean enabled);

    void pauseRule(Long id);

    void resumeRule(Long id);

    void triggerNow(Long id);

    /**
     * Test run: executes query + evaluation but does NOT persist or deliver. Returns list of
     * would-be alert events for preview.
     */
    List<AlertEvaluator.AlertEventCandidate> testRun(Long id) throws Exception;

    Page<AlertExecutionDO> getExecutionList(Page<AlertExecutionDO> page, Long ruleId,
            String status);

    Page<AlertEventDO> getEventList(Page<AlertEventDO> page, Long ruleId, String severity,
            String deliveryStatus, String resolutionStatus);

    AlertEventDO getEventById(Long eventId);

    AlertEventDO transitionEvent(Long eventId, AlertEventTransitionReq req, User user);

    Map<Long, Long> countPendingEventsByRule();
}
