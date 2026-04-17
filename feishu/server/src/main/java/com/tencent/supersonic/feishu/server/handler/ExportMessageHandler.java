package com.tencent.supersonic.feishu.server.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tencent.supersonic.common.pojo.QueryColumn;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.api.pojo.FeishuMessage;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuQuerySessionDO;
import com.tencent.supersonic.feishu.server.persistence.mapper.FeishuQuerySessionMapper;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import com.tencent.supersonic.feishu.server.service.SuperSonicApiClient;
import com.tencent.supersonic.headless.api.pojo.response.SemanticQueryResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@ConditionalOnProperty(name = "s2.feishu.enabled", havingValue = "true")
@Component
@Slf4j
@RequiredArgsConstructor
public class ExportMessageHandler implements MessageHandler {

    private static final long MAX_FILE_SIZE = 30 * 1024 * 1024; // 30MB Feishu limit

    private final FeishuQuerySessionMapper sessionMapper;
    private final FeishuMessageSender messageSender;
    private final SuperSonicApiClient apiClient;

    @Override
    public void handle(FeishuMessage msg, User user) {
        // 1. Find the most recent SUCCESS session with sqlText and datasetId
        LambdaQueryWrapper<FeishuQuerySessionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeishuQuerySessionDO::getFeishuOpenId, msg.getOpenId())
                .eq(FeishuQuerySessionDO::getStatus, "SUCCESS")
                .isNotNull(FeishuQuerySessionDO::getSqlText)
                .isNotNull(FeishuQuerySessionDO::getDatasetId)
                .orderByDesc(FeishuQuerySessionDO::getCreatedAt).last("LIMIT 1");
        FeishuQuerySessionDO session = sessionMapper.selectOne(wrapper);

        if (session == null) {
            messageSender.replyText(msg.getMessageId(), "没有找到最近的查询记录，请先进行一次查询");
            return;
        }

        // 2. Immediate feedback
        messageSender.replyText(msg.getMessageId(),
                "正在导出「" + session.getQueryText() + "」的查询结果，请稍候...");

        File csvFile = null;
        try {
            // 3. Re-run the SQL query for full data
            SemanticQueryResp resp =
                    apiClient.queryBySql(session.getSqlText(), session.getDatasetId(), user);

            if (resp == null || resp.getResultList() == null || resp.getResultList().isEmpty()) {
                messageSender.sendText(msg.getOpenId(), "查询结果为空，无法导出");
                return;
            }

            // 4. Write CSV to temp file
            csvFile = writeCsv(resp.getColumns(), resp.getResultList());

            // 5. Check file size
            if (csvFile.length() > MAX_FILE_SIZE) {
                messageSender.sendText(msg.getOpenId(), "导出文件超过30MB限制，请缩小查询范围后重试");
                return;
            }

            // 6. Upload to Feishu
            String fileKey = messageSender.uploadFile(csvFile, "export.csv", "stream");

            // 7. Send file message
            messageSender.sendFile(msg.getOpenId(), fileKey);

        } catch (Exception e) {
            log.error("Export failed for openId={}, session={}", msg.getOpenId(), session.getId(),
                    e);
            messageSender.sendText(msg.getOpenId(), "导出失败: " + e.getMessage());
        } finally {
            if (csvFile != null && csvFile.exists()) {
                csvFile.delete();
            }
        }
    }

    private File writeCsv(List<QueryColumn> columns, List<Map<String, Object>> rows)
            throws Exception {
        File file = File.createTempFile("feishu_export_", ".csv");
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            // BOM for Excel UTF-8 compatibility
            writer.write('\uFEFF');

            // Header row
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    writer.write(',');
                }
                QueryColumn col = columns.get(i);
                String header = col.getName() != null ? col.getName() : col.getNameEn();
                writer.write(escapeCsv(header));
            }
            writer.newLine();

            // Data rows — use nameEn (bizName) as the map key
            for (Map<String, Object> row : rows) {
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) {
                        writer.write(',');
                    }
                    String key = columns.get(i).getNameEn();
                    Object value = row.get(key);
                    writer.write(escapeCsv(value != null ? value.toString() : ""));
                }
                writer.newLine();
            }
        }
        return file;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
