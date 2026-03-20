package com.tencent.supersonic.chat.server.parser;

import com.tencent.supersonic.chat.api.plugin.PluginParseResult;
import com.tencent.supersonic.chat.server.plugin.support.reportschedule.ReportScheduleQuery;
import com.tencent.supersonic.chat.server.plugin.support.reportschedule.ScheduleKeywords;
import com.tencent.supersonic.chat.server.pojo.ParseContext;
import com.tencent.supersonic.common.pojo.Constants;
import com.tencent.supersonic.headless.api.pojo.SemanticParseInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Keyword-based parser for report schedule intents. Runs without LLM and without a DB-registered
 * plugin, so scheduled-report commands work for any agent out of the box.
 *
 * <p>
 * AI-generated — reviewed 2026-03-20.
 */
public class ReportScheduleParser implements ChatQueryParser {

    @Override
    public boolean accept(ParseContext parseContext) {
        String text = parseContext.getRequest().getQueryText();
        if (text == null) {
            return false;
        }
        String lower = text.trim().toLowerCase();

        if (ScheduleKeywords.preferCreate(lower)) {
            return true;
        }

        if (ScheduleKeywords.preferList(lower)) {
            return true;
        }

        // STATUS: "执行情况", "执行记录"
        if (ScheduleKeywords.STATUS.stream().anyMatch(lower::contains)) {
            return true;
        }

        // CANCEL/PAUSE/RESUME/TRIGGER: only when a schedule ID (#N) is present to avoid
        // false positives from common words like "取消", "暂停"
        if (ScheduleKeywords.SCHEDULE_ID_PATTERN.matcher(text).find()) {
            if (ScheduleKeywords.CANCEL.stream().anyMatch(lower::contains))
                return true;
            if (ScheduleKeywords.PAUSE.stream().anyMatch(lower::contains))
                return true;
            if (ScheduleKeywords.RESUME.stream().anyMatch(lower::contains))
                return true;
            if (ScheduleKeywords.TRIGGER.stream().anyMatch(lower::contains))
                return true;
        }

        // CONFIRM: pure confirmation word + pending confirmation exists for this user+chat
        if (ScheduleKeywords.CONFIRM.stream().anyMatch(lower::equals)) {
            Long userId = parseContext.getRequest().getUser() != null
                    ? parseContext.getRequest().getUser().getId()
                    : 0L;
            Integer chatId = parseContext.getRequest().getChatId();
            return ReportScheduleQuery.hasPendingConfirmation(userId, chatId);
        }

        return false;
    }

    @Override
    public void parse(ParseContext parseContext) {
        // Guard: NL2PluginParser may have already routed this query via a DB-registered plugin.
        boolean alreadyRouted = parseContext.getResponse().getSelectedParses().stream()
                .anyMatch(p -> ReportScheduleQuery.QUERY_MODE.equals(p.getQueryMode()));
        if (alreadyRouted) {
            return;
        }

        SemanticParseInfo parseInfo = new SemanticParseInfo();
        parseInfo.setQueryMode(ReportScheduleQuery.QUERY_MODE);
        parseInfo.setId(1);
        parseInfo.setScore(0.9);
        parseInfo.setTextInfo("定时报表管理");

        PluginParseResult pluginParseResult = new PluginParseResult();
        pluginParseResult.setQueryText(parseContext.getRequest().getQueryText());
        pluginParseResult.setChatId(parseContext.getRequest().getChatId());
        pluginParseResult.setQueryId(parseContext.getRequest().getQueryId());
        if (parseContext.getRequest().getUser() != null) {
            pluginParseResult.setUserId(parseContext.getRequest().getUser().getId());
            pluginParseResult.setUserName(parseContext.getRequest().getUser().getName());
            pluginParseResult.setTenantId(parseContext.getRequest().getUser().getTenantId());
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(Constants.CONTEXT, pluginParseResult);
        parseInfo.setProperties(properties);

        parseContext.getResponse().getSelectedParses().add(parseInfo);
    }
}
