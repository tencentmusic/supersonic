package com.tencent.supersonic.chat.server.service.impl;


import com.tencent.supersonic.chat.api.pojo.request.TextVoiceReq;
import com.tencent.supersonic.chat.server.pojo.IATResult;
import com.tencent.supersonic.chat.server.service.VoiceService;
import com.tencent.supersonic.common.util.HttpUtils;
import com.tencent.supersonic.common.util.JsonUtil;
import com.tencent.supersonic.common.util.MiguApiUrlUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VoiceServiceImpl implements VoiceService {

    @Value("${lingxiyun.appId:APP_ID}")
    private String appId;
    @Value("${lingxiyun.secretKey:SECRET_KEY}")
    private String secretKey;
    @Value("${lingxiyun.host:HOST}")
    private String host;
    @Value("${lingxiyun.iatUrl:IAT_URL}")
    private String iatUrl;
    @Value("${lingxiyun.ttsUrl:TTS_URL}")
    private String ttsUrl;
    @Value("${bi.tts.base-path}")
    private String basePath;
    @Value("${bi.tts.base-url}")
    private String baseUrl;

    @Override
    public String voiceText(byte[] data) {
        Map<String, String> headers = new HashMap<>();
        String sid = UUID.randomUUID().toString().replace("-", "");
        headers.put("sid", sid);
        Map<String, String> sessionParam = new HashMap<>();
        sessionParam.put("rate", "16k");
        sessionParam.put("rst", "plain");
        headers.put("sessionParam",
                Base64.getEncoder().encodeToString(JsonUtil.toString(sessionParam).getBytes()));
        headers.put("endFlag", "1");
        try {
            Map<String, Object> map = new HashMap<>();
            String urlpath = MiguApiUrlUtils.doSignature(iatUrl, "post", map, appId, secretKey);
            IATResult result = HttpUtils.post(host + urlpath, data, headers, IATResult.class);
            if (!"OK".equals(result.getState())) {
                log.warn("语音识别接口返回错误: {}", JsonUtil.toString(result));
                return null;
            }

            return result.getBody().stream().map(IATResult.FrameResult::getAnsStr)
                    .collect(Collectors.joining(""));

        } catch (Exception e) {
            log.error("语音识别出错", e);
            return null;
        }
    }

    @Override
    public String textVoice(TextVoiceReq textVoiceReq) {
        Map<String, String> headers = new HashMap<>();
        String sid = UUID.randomUUID().toString().replace("-", "");
        headers.put("sid", sid);
        Map<String, String> sessionParam = new HashMap<>();
        sessionParam.put("native_voice_name", "qianxue2");
        sessionParam.put("sample_rate", "16000");
        sessionParam.put("audio_coding", "mp3");
        headers.put("sessionParam",
                Base64.getEncoder().encodeToString(JsonUtil.toString(sessionParam).getBytes()));
        try {
            Map<String, Object> map = new HashMap<>();
            String urlpath = MiguApiUrlUtils.doSignature(ttsUrl, "post", map, appId, secretKey);

            Response response = HttpUtils.postWithReponse(host + urlpath,
                    JsonUtil.toString(textVoiceReq), headers);

            if (!"application/octet-stream".equals(response.header("Content-Type"))) {
                log.warn("语音合成出错:" + response.body().string());
                return null;
            }
            byte[] data = response.body().bytes();
            String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String path = "/" + day + "/" + uuid + ".mp3";
            File file = new File(basePath + path);
            file.getParentFile().mkdirs();
            FileCopyUtils.copy(data, file);
            return baseUrl + path;
        } catch (Exception e) {
            log.error("语音合成出错", e);
            return null;
        }
    }

}
