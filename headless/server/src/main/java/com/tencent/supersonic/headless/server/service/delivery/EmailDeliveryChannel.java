package com.tencent.supersonic.headless.server.service.delivery;

import com.alibaba.fastjson.JSON;
import com.tencent.supersonic.headless.server.pojo.DeliveryType;
import jakarta.mail.internet.MimeMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Email delivery channel. Sends report as email attachment using Spring Mail.
 */
@Component
@Slf4j
public class EmailDeliveryChannel implements ReportDeliveryChannel {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Override
    public DeliveryType getType() {
        return DeliveryType.EMAIL;
    }

    @Override
    public void deliver(String configJson, DeliveryContext context) throws DeliveryException {
        if (mailSender == null) {
            throw new DeliveryException("Email is not configured. Please configure spring.mail.*",
                    false);
        }

        EmailConfig config = parseConfig(configJson);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(
                    StringUtils.isNotBlank(config.getFrom()) ? config.getFrom() : fromAddress);
            helper.setTo(config.getTo().toArray(new String[0]));

            if (config.getCc() != null && !config.getCc().isEmpty()) {
                helper.setCc(config.getCc().toArray(new String[0]));
            }

            // Build subject
            String subject = config.getSubject();
            if (StringUtils.isBlank(subject)) {
                subject = String.format("Report: %s - %s", context.getReportName(),
                        context.getExecutionTime());
            } else {
                subject = subject.replace("${reportName}", context.getReportName())
                        .replace("${executionTime}", context.getExecutionTime());
            }
            helper.setSubject(subject);

            // Build body
            String body = config.getBody();
            if (StringUtils.isBlank(body)) {
                body = String.format(
                        "Your scheduled report has been generated.\n\n"
                                + "Report: %s\nExecution Time: %s\nRows: %d\n\n"
                                + "Please find the report attached.",
                        context.getReportName(), context.getExecutionTime(), context.getRowCount());
            } else {
                body = body.replace("${reportName}", context.getReportName())
                        .replace("${executionTime}", context.getExecutionTime())
                        .replace("${rowCount}", String.valueOf(context.getRowCount()));
            }
            helper.setText(body, config.getHtmlBody() != null && config.getHtmlBody());

            // Attach file
            if (StringUtils.isNotBlank(context.getFileLocation())) {
                File file = new File(context.getFileLocation());
                if (file.exists()) {
                    FileSystemResource attachment = new FileSystemResource(file);
                    helper.addAttachment(file.getName(), attachment);
                }
            }

            mailSender.send(message);

            log.info("Email delivery successful: to={}, scheduleId={}", config.getTo(),
                    context.getScheduleId());

        } catch (Exception e) {
            log.error("Email delivery failed: to={}", config.getTo(), e);
            throw new DeliveryException("Email delivery failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateConfig(String configJson) {
        EmailConfig config = parseConfig(configJson);
        if (config.getTo() == null || config.getTo().isEmpty()) {
            throw new IllegalArgumentException("Email recipients (to) are required");
        }
        for (String email : config.getTo()) {
            if (!email.contains("@")) {
                throw new IllegalArgumentException("Invalid email address: " + email);
            }
        }
        return true;
    }

    private EmailConfig parseConfig(String configJson) {
        if (StringUtils.isBlank(configJson)) {
            throw new IllegalArgumentException("Email config is required");
        }
        try {
            return JSON.parseObject(configJson, EmailConfig.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid email config JSON: " + e.getMessage());
        }
    }

    @Data
    public static class EmailConfig {
        private String from;
        private List<String> to;
        private List<String> cc;
        private String subject;
        private String body;
        private Boolean htmlBody;
    }
}
