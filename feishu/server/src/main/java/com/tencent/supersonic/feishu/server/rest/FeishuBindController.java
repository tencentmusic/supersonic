package com.tencent.supersonic.feishu.server.rest;

import com.tencent.supersonic.auth.api.authentication.annotation.AuthenticationIgnore;
import com.tencent.supersonic.common.pojo.User;
import com.tencent.supersonic.feishu.api.config.FeishuProperties;
import com.tencent.supersonic.feishu.server.persistence.dataobject.FeishuUserMappingDO;
import com.tencent.supersonic.feishu.server.service.FeishuBindTokenService;
import com.tencent.supersonic.feishu.server.service.FeishuBindTokenService.BindTokenPayload;
import com.tencent.supersonic.feishu.server.service.FeishuMessageSender;
import com.tencent.supersonic.feishu.server.service.FeishuUserMappingService;
import com.tencent.supersonic.feishu.server.service.SuperSonicApiClient;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/feishu")
@RequiredArgsConstructor
@Slf4j
public class FeishuBindController {

    private final FeishuBindTokenService bindTokenService;
    private final FeishuUserMappingService mappingService;
    private final SuperSonicApiClient apiClient;
    private final FeishuMessageSender messageSender;
    private final FeishuProperties properties;

    @Data
    public static class FeishuBindRequest {
        private String bindToken;
        private String username;
        private String password;
    }

    @Data
    public static class FeishuBindResult {
        private boolean success;
        private String s2UserName;
        private String message;

        public static FeishuBindResult ok(String s2UserName) {
            FeishuBindResult r = new FeishuBindResult();
            r.success = true;
            r.s2UserName = s2UserName;
            r.message = "绑定成功";
            return r;
        }

        public static FeishuBindResult fail(String message) {
            FeishuBindResult r = new FeishuBindResult();
            r.success = false;
            r.message = message;
            return r;
        }
    }

    /**
     * Serve the H5 bind page. Validates the token before returning the page.
     */
    @GetMapping("/bindPage")
    @AuthenticationIgnore
    public ResponseEntity<String> bindPage(@RequestParam String token) {
        // Validate token before serving the page
        BindTokenPayload payload = bindTokenService.validateToken(token);
        if (payload == null) {
            return ResponseEntity.status(HttpStatus.OK).contentType(MediaType.TEXT_HTML)
                    .body(errorPageHtml("绑定链接已过期或无效，请在飞书对话中重新发送任意消息获取新链接。"));
        }

        try {
            ClassPathResource resource = new ClassPathResource("static/feishu/bind.html");
            String html = resource.getContentAsString(StandardCharsets.UTF_8);
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        } catch (IOException e) {
            log.error("Failed to load bind page", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_HTML).body(errorPageHtml("页面加载失败，请稍后重试。"));
        }
    }

    /**
     * Handle the bind request: verify token + credentials, then complete the mapping.
     */
    @PostMapping("/bind")
    @AuthenticationIgnore
    public ResponseEntity<FeishuBindResult> bind(@RequestBody FeishuBindRequest request) {
        // 1. Validate bindToken
        BindTokenPayload payload = bindTokenService.validateToken(request.getBindToken());
        if (payload == null) {
            return ResponseEntity.badRequest()
                    .body(FeishuBindResult.fail("绑定链接已过期，请在飞书对话中重新发送任意消息获取新链接"));
        }

        // 2. Verify SuperSonic credentials
        User user = apiClient.login(request.getUsername(), request.getPassword(),
                payload.getTenantId());
        if (user == null) {
            return ResponseEntity.ok(FeishuBindResult.fail("账号或密码错误，请重试"));
        }

        // 3. Check if SuperSonic user is already bound to another Feishu account
        FeishuUserMappingDO existing = mappingService.findByS2UserId(user.getId());
        if (existing != null && !existing.getFeishuOpenId().equals(payload.getOpenId())) {
            return ResponseEntity.ok(FeishuBindResult.fail("该平台账号已被其他飞书用户绑定，请联系管理员处理"));
        }

        // 4. Complete binding
        mappingService.completeBinding(payload.getMappingId(), user.getId(), user.getTenantId());

        // 5. Mark token as used
        bindTokenService.markUsed(request.getBindToken());

        // 6. Notify user via Feishu message
        try {
            messageSender.sendText(payload.getOpenId(),
                    "绑定成功！您的飞书账号已关联平台用户「" + user.getDisplayName() + "」，现在可以开始查询了。");
        } catch (Exception e) {
            log.warn("Failed to send bind success notification: {}", e.getMessage());
        }

        return ResponseEntity.ok(FeishuBindResult.ok(user.getDisplayName()));
    }

    private String errorPageHtml(String message) {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>绑定失败</title>
                <style>
                body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;display:flex;justify-content:center;
                align-items:center;min-height:100vh;margin:0;background:#f5f5f5}
                .card{background:#fff;border-radius:12px;padding:32px;max-width:400px;width:90%%;text-align:center;
                box-shadow:0 2px 12px rgba(0,0,0,.08)}
                .msg{color:#666;font-size:16px;line-height:1.6}
                </style>
                </head>
                <body><div class="card"><p class="msg">%s</p></div></body>
                </html>
                """
                .formatted(escapeHtml(message));
    }

    private static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
