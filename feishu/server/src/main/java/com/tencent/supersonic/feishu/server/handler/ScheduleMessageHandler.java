package com.tencent.supersonic.feishu.server.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.api.pojo.FeishuMessage;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuQuerySessionDO;
import com.tencent.supersonic.feishu.server.persistence.mapper.FeishuQuerySessionMapper;
import com.tencent.supersonic.feishu.server.render.FeishuCardRenderer;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportDeliveryConfigDO;
import com.tencent.supersonic.headless.server.persistence.dataobject.ReportScheduleDO;
import com.tencent.supersonic.headless.server.service.ReportDeliveryService;
import com.tencent.supersonic.headless.server.service.ReportScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduleMessageHandler implements MessageHandler {

    private final FeishuQuerySessionMapper sessionMapper;
    private final FeishuMessageSender messageSender;
    private final FeishuCardRenderer cardRenderer;
    private final ReportScheduleService reportScheduleService;
    private final ReportDeliveryService reportDeliveryService;

    @Override
    public void handle(FeishuMessage msg, User user) {
        String text = msg.getContent() != null ? msg.getContent().trim() : "";
        String lower = text.toLowerCase();

        // LIST intent: /schedule list OR /schedule
        if (lower.equals("/schedule") || lower.equals("/schedule list") || lower.contains("调度列表")
                || lower.contains("我的调度")) {
            handleList(msg, user);
            return;
        }

        // CREATE intent: /schedule create OR natural language patterns
        if (lower.startsWith("/schedule create") || lower.startsWith("/schedule 每")
                || lower.startsWith("/schedule daily") || lower.startsWith("/schedule weekly")) {
            handleCreate(msg, user, text);
            return;
        }

        // PAUSE: /schedule pause <id>
        if (lower.startsWith("/schedule pause ")) {
            handlePause(msg, user, text);
            return;
        }

        // RESUME: /schedule resume <id>
        if (lower.startsWith("/schedule resume ")) {
            handleResume(msg, user, text);
            return;
        }

        // CANCEL/DELETE: /schedule cancel <id> OR /schedule delete <id>
        if (lower.startsWith("/schedule cancel ") || lower.startsWith("/schedule delete ")) {
            handleCancel(msg, user, text);
            return;
        }

        // Default: show help for /schedule
        messageSender.replyText(msg.getMessageId(),
                "定时报表命令：\n" + "• `/schedule` - 查看调度列表\n" + "• `/schedule 每天9点` - 基于最近查询创建每日定时任务\n"
                        + "• `/schedule pause <id>` - 暂停任务\n" + "• `/schedule resume <id>` - 恢复任务\n"
                        + "• `/schedule cancel <id>` - 删除任务");
    }

    private void handleList(FeishuMessage msg, User user) {
        try {
            Page<ReportScheduleDO> page =
                    reportScheduleService.getScheduleList(new Page<>(1, 20), null, null);
            Map<String, Object> card = cardRenderer.renderScheduleListCard(page.getRecords());
            messageSender.replyCard(msg.getMessageId(), card);
        } catch (Exception e) {
            log.error("Failed to list schedules", e);
            messageSender.replyText(msg.getMessageId(), "获取调度列表失败: " + e.getMessage());
        }
    }

    private void handleCreate(FeishuMessage msg, User user, String text) {
        try {
            // 1. Find most recent successful session for this user
            LambdaQueryWrapper<FeishuQuerySessionDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FeishuQuerySessionDO::getFeishuOpenId, msg.getOpenId())
                    .eq(FeishuQuerySessionDO::getStatus, "SUCCESS")
                    .isNotNull(FeishuQuerySessionDO::getDatasetId)
                    .orderByDesc(FeishuQuerySessionDO::getCreatedAt).last("LIMIT 1");
            FeishuQuerySessionDO session = sessionMapper.selectOne(wrapper);

            if (session == null) {
                messageSender.replyText(msg.getMessageId(),
                        "没有找到最近的查询记录，请先进行一次查询，再使用 /schedule 创建定时任务");
                return;
            }

            // 2. Parse cron expression from text (simple patterns)
            String cron = parseCron(text);
            if (cron == null) {
                messageSender.replyText(msg.getMessageId(), "未识别调度频率，请使用如「每天9点」「每周一」等表达方式");
                return;
            }

            // 3. Find default delivery config
            String deliveryConfigIds = findDefaultDeliveryConfigId();
            if (deliveryConfigIds == null) {
                messageSender.replyText(msg.getMessageId(), "暂无可用的推送渠道，请先在后台配置推送渠道");
                return;
            }

            // 4. Create schedule
            ReportScheduleDO schedule = new ReportScheduleDO();
            schedule.setName("定时报表-" + session.getQueryText());
            schedule.setDatasetId(session.getDatasetId());
            schedule.setCronExpression(cron);
            schedule.setOutputFormat("EXCEL");
            schedule.setDeliveryConfigIds(deliveryConfigIds);
            schedule.setEnabled(true);
            schedule.setRetryCount(3);
            schedule.setOwnerId(user.getId());
            schedule.setTenantId(user.getTenantId());
            schedule.setCreatedBy(user.getName());

            ReportScheduleDO created = reportScheduleService.createSchedule(schedule);

            Map<String, Object> card =
                    cardRenderer.renderScheduleCreatedCard(created, describeCron(cron));
            messageSender.replyCard(msg.getMessageId(), card);
        } catch (Exception e) {
            log.error("Failed to create schedule", e);
            messageSender.replyText(msg.getMessageId(), "创建定时任务失败: " + e.getMessage());
        }
    }

    private void handlePause(FeishuMessage msg, User user, String text) {
        Long id = extractId(text);
        if (id == null) {
            messageSender.replyText(msg.getMessageId(), "请指定任务ID，如: /schedule pause 123");
            return;
        }
        try {
            reportScheduleService.pauseSchedule(id);
            messageSender.replyText(msg.getMessageId(), "已暂停任务 #" + id);
        } catch (Exception e) {
            log.error("Failed to pause schedule id={}", id, e);
            messageSender.replyText(msg.getMessageId(), "暂停失败: " + e.getMessage());
        }
    }

    private void handleResume(FeishuMessage msg, User user, String text) {
        Long id = extractId(text);
        if (id == null) {
            messageSender.replyText(msg.getMessageId(), "请指定任务ID，如: /schedule resume 123");
            return;
        }
        try {
            reportScheduleService.resumeSchedule(id);
            messageSender.replyText(msg.getMessageId(), "已恢复任务 #" + id);
        } catch (Exception e) {
            log.error("Failed to resume schedule id={}", id, e);
            messageSender.replyText(msg.getMessageId(), "恢复失败: " + e.getMessage());
        }
    }

    private void handleCancel(FeishuMessage msg, User user, String text) {
        Long id = extractId(text);
        if (id == null) {
            messageSender.replyText(msg.getMessageId(), "请指定任务ID，如: /schedule cancel 123");
            return;
        }
        try {
            reportScheduleService.deleteSchedule(id);
            messageSender.replyText(msg.getMessageId(), "已删除任务 #" + id);
        } catch (Exception e) {
            log.error("Failed to delete schedule id={}", id, e);
            messageSender.replyText(msg.getMessageId(), "删除失败: " + e.getMessage());
        }
    }

    private String parseCron(String text) {
        String lower = text.toLowerCase();
        // 每小时
        if (lower.contains("每小时"))
            return "0 0 * * * ?";
        // 每天 (default hour 9)
        if (lower.contains("每天") || lower.contains("daily")) {
            int hour = extractHour(lower, 9);
            return String.format("0 0 %d * * ?", hour);
        }
        // 每周/每周一
        if (lower.contains("每周") || lower.contains("weekly"))
            return "0 0 9 ? * 2"; // Monday 9am
        // 每月
        if (lower.contains("每月") || lower.contains("monthly"))
            return "0 0 9 1 * ?"; // 1st of month
        return null;
    }

    private int extractHour(String text, int defaultHour) {
        Matcher m = Pattern.compile("(\\d{1,2})点").matcher(text);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            return (h >= 1 && h <= 23) ? h : defaultHour;
        }
        return defaultHour;
    }

    private String describeCron(String cron) {
        if (cron == null)
            return "未知频率";
        String[] p = cron.split(" ");
        if (p.length < 6)
            return cron;
        String hour = p[2];
        String dom = p[3];
        String dow = p[5];
        if (!"?".equals(dow) && !"*".equals(dow))
            return "每周 " + hour + ":00";
        if (!"*".equals(dom) && !"?".equals(dom))
            return "每月" + dom + "日 " + hour + ":00";
        if ("*".equals(hour))
            return "每小时";
        return "每天 " + hour + ":00";
    }

    private Long extractId(String text) {
        Matcher m = Pattern.compile("(\\d+)").matcher(text);
        if (m.find())
            return Long.parseLong(m.group(1));
        return null;
    }

    private String findDefaultDeliveryConfigId() {
        try {
            List<ReportDeliveryConfigDO> configs =
                    reportDeliveryService.getConfigList(new Page<>(1, 100)).getRecords().stream()
                            .filter(c -> Boolean.TRUE.equals(c.getEnabled())).toList();
            return configs.isEmpty() ? null : String.valueOf(configs.get(0).getId());
        } catch (Exception e) {
            log.warn("Failed to find delivery config", e);
            return null;
        }
    }
}
